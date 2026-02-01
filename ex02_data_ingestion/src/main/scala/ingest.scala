import java.util.Properties
import org.apache.spark.sql.DataFrame
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
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

    val basePath = "s3a://nyc-validated/2023/"

    val df = spark.read.parquet(basePath)

  val datetimeDf = df
      .select(
        col("tpep_pickup_datetime").as("full_timestamp")
      )
      .union(
        df.select(col("tpep_dropoff_datetime").as("full_timestamp"))
      )
      .filter(col("full_timestamp").isNotNull)
      .distinct()

      val dimDatetime = datetimeDf
        .withColumn("year", year(col("full_timestamp")))
        .withColumn("month", month(col("full_timestamp")))
        .withColumn("day", dayofmonth(col("full_timestamp")))
        .withColumn("hour", hour(col("full_timestamp")))
        .withColumn("day_of_week", dayofweek(col("full_timestamp")))
        .withColumn("is_weekend", col("day_of_week").isin(1, 7))

      dimDatetime.write
        .mode("append")
        .jdbc(jdbcUrl, "dim_datetime", jdbcProps)

        
      val dimDatetimePg = spark.read.format("jdbc")
          .option("url", jdbcUrl)
          .option("dbtable", "dim_datetime")
          .option("user", jdbcProps.getProperty("user"))
          .option("password", jdbcProps.getProperty("password"))
          .option("driver", "org.postgresql.Driver")
          .option("numPartitions", "100")  // Number of parallel connections
          .option("partitionColumn", "datetime_id")  // Numeric column for partitioning
          .option("lowerBound", "1")
          .option("upperBound", "25000000")
          .load()
          .select("datetime_id", "full_timestamp")

      val dimPickup = dimDatetimePg
        .withColumnRenamed("datetime_id", "pickup_datetime_id")
        .alias("pickup")

      val dimDropoff = dimDatetimePg
        .withColumnRenamed("datetime_id", "dropoff_datetime_id")
        .alias("dropoff")

      val dfNormalized = df
        .withColumn("VendorID", col("VendorID").cast("long").cast("int"))
        .withColumn("RatecodeID", col("RatecodeID").cast("long").cast("int"))
        .withColumn("payment_type", col("payment_type").cast("long").cast("int"))

      val dfTimestamps = dfNormalized
        .withColumn("tpep_pickup_datetime", col("tpep_pickup_datetime").cast("timestamp"))
        .withColumn("tpep_dropoff_datetime", col("tpep_dropoff_datetime").cast("timestamp"))

      val fact = dfTimestamps
        .join(dimPickup, dfTimestamps("tpep_pickup_datetime") === dimPickup("full_timestamp"))
        .join(dimDropoff, dfTimestamps("tpep_dropoff_datetime") === dimDropoff("full_timestamp"))
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
      fact.write
        .mode("append")
        .jdbc(jdbcUrl, "fact_trip", jdbcProps)
        
    }

}
