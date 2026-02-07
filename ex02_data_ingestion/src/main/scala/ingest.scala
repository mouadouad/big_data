import java.util.Properties
import org.apache.spark.sql.DataFrame
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{broadcast, col, year, month, dayofmonth, hour, dayofweek}
import java.sql.DriverManager
import org.apache.spark.sql.Row


object Ingestor{
 
  def copyLookUp(spark: SparkSession, jdbcUrl: String, jdbcProps: Properties) : Unit = {
   val zonesDf = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("../data/external/taxi_zone_lookup.csv")
      .select(
        col("LocationID").as("zone_id"),
        col("Borough").as("borough"),
        col("Zone").as("zone_name")
      )

    zonesDf.write
      .mode("append") 
      .jdbc(jdbcUrl, "dim_zone", jdbcProps)
  }

  def ingestData(spark: SparkSession, jdbcUrl: String, jdbcProps: Properties) : Unit = {
    
    println("============================================================")
    println("Reading ALL validated data for 2023")
    println("============================================================")

    // The validator writes all months to s3a://nyc-validated/2023/ as one dataset
    // So we read it all at once (not month-by-month files)
    val basePath = "s3a://nyc-validated/2023/"
    
    println(s"\n[1/4] Reading validated data from $basePath")
    val dfAll = spark.read.parquet(basePath)
    val totalRows = dfAll.count()
    println(s"  Total validated rows: $totalRows")

    // Extract unique timestamps from ALL data
    println("\n[2/4] Extracting unique timestamps...")
    val datetimeDf = dfAll
      .select(col("tpep_pickup_datetime").as("full_timestamp"))
      .union(dfAll.select(col("tpep_dropoff_datetime").as("full_timestamp")))
      .filter(col("full_timestamp").isNotNull)
      .distinct()

    val uniqueTimestamps = datetimeDf.count()
    println(s"  Unique timestamps: $uniqueTimestamps")

    // SKIP: dim_datetime already populated with 24M rows from previous run
    // Writing would cause duplicate key errors
    println("  Skipping dim_datetime write (already populated)")
    
    /*
    // Create dimension records (database will skip duplicates via unique constraint)
    val dimDatetime = datetimeDf
      .withColumn("year", year(col("full_timestamp")))
      .withColumn("month", month(col("full_timestamp")))
      .withColumn("day", dayofmonth(col("full_timestamp")))
      .withColumn("hour", hour(col("full_timestamp")))
      .withColumn("day_of_week", dayofweek(col("full_timestamp")))
      .withColumn("is_weekend", col("day_of_week").isin(1, 7))

    println("  Writing timestamps to dim_datetime (may skip duplicates)...")
    dimDatetime.write
      .mode("append")
      .jdbc(jdbcUrl, "dim_datetime", jdbcProps)
    println("  ✓ Timestamps inserted")
    */

    // Read back datetime dimension with IDs
    println("\n[3/4] Reading datetime dimension from PostgreSQL...")
    val dimDatetimePg = spark.read.format("jdbc")
      .option("url", jdbcUrl)
      .option("dbtable", "dim_datetime")
      .option("user", jdbcProps.getProperty("user"))
      .option("password", jdbcProps.getProperty("password"))
      .option("driver", "org.postgresql.Driver")
      .load()
      .select("datetime_id", "full_timestamp")

    println(s"  Loaded ${dimDatetimePg.count()} datetime records")

    // Broadcast the datetime dimension (24M rows - fits in memory with 16GB heap)
    val dimPickup = broadcast(dimDatetimePg
      .withColumnRenamed("datetime_id", "pickup_datetime_id")
      .alias("pickup"))

    val dimDropoff = broadcast(dimDatetimePg
      .withColumnRenamed("datetime_id", "dropoff_datetime_id")
      .alias("dropoff"))

    println("\n[4/4] Joining trip data with dimensions and writing to PostgreSQL...")
    
    // Normalize data types
    val dfNormalized = dfAll
      .withColumn("VendorID", col("VendorID").cast("long").cast("int"))
      .withColumn("RatecodeID", col("RatecodeID").cast("long").cast("int"))
      .withColumn("payment_type", col("payment_type").cast("long").cast("int"))
      .withColumn("tpep_pickup_datetime", col("tpep_pickup_datetime").cast("timestamp"))
      .withColumn("tpep_dropoff_datetime", col("tpep_dropoff_datetime").cast("timestamp"))

    // Join with dimension tables to get IDs
    val fact = dfNormalized
      .join(dimPickup, dfNormalized("tpep_pickup_datetime") === dimPickup("full_timestamp"), "left")
      .join(dimDropoff, dfNormalized("tpep_dropoff_datetime") === dimDropoff("full_timestamp"), "left")
      .select(
        col("pickup.pickup_datetime_id"),
        col("dropoff.dropoff_datetime_id"),
        col("PULocationID").as("pickup_zone_id"),
        col("DOLocationID").as("dropoff_zone_id"),
        col("VendorID").as("vendor_id"),
        col("RatecodeID").as("ratecode_id"),
        col("payment_type").as("payment_type_id"),
        col("passenger_count"),
        col("trip_distance"),
        col("fare_amount"),
        col("extra"),
        col("mta_tax"),
        col("tip_amount"),
        col("tolls_amount"),
        col("improvement_surcharge"),
        col("congestion_surcharge"),
        col("airport_fee"),
        col("total_amount")
      )

    // Repartition to optimize PostgreSQL writes (use 8 partitions for parallelism)
    println("  Repartitioning for efficient PostgreSQL writes...")
    val factRepartitioned = fact.repartition(8)

    // Write to PostgreSQL
    println("  Writing to fact_trip table...")
    factRepartitioned.write
      .mode("append")
      .jdbc(jdbcUrl, "fact_trip", jdbcProps)

    println(s"  ✓ Successfully wrote $totalRows trips to fact_trip")
    println("\n============================================================")
    println("✓ Data ingestion complete!")
    println("============================================================")
  }

}
