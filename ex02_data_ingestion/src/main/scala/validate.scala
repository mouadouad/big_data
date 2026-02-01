import org.apache.spark.sql.{SparkSession, DataFrame, functions => F}
import org.apache.spark.sql.types._

object Validator {
  def validate(spark: SparkSession): Unit = {
    
    val bucket = "nyc-raw"
    val validatedBucket = "nyc-validated"
    val year = 2023

    val vendorIDs = Seq(1, 2, 6, 7)
    val rateCodes = Seq(1, 2, 3, 4, 5, 6, 99)
    val storeAndFwdFlags = Seq("Y", "N")
    val paymentTypes = Seq(0, 1, 2, 3, 4, 5, 6)

    val targetSchema = StructType(Seq(
      StructField("VendorID", IntegerType, true),
      StructField("tpep_pickup_datetime", TimestampType, true),
      StructField("tpep_dropoff_datetime", TimestampType, true),
      StructField("passenger_count", IntegerType, true),
      StructField("trip_distance", DoubleType, true),
      StructField("RatecodeID", IntegerType, true),
      StructField("store_and_fwd_flag", StringType, true),
      StructField("PULocationID", LongType, true),
      StructField("DOLocationID", LongType, true),
      StructField("payment_type", IntegerType, true),
      StructField("fare_amount", DoubleType, true),
      StructField("extra", DoubleType, true),
      StructField("mta_tax", DoubleType, true),
      StructField("tip_amount", DoubleType, true),
      StructField("tolls_amount", DoubleType, true),
      StructField("improvement_surcharge", DoubleType, true),
      StructField("total_amount", DoubleType, true),
      StructField("congestion_surcharge", DoubleType, true),
      StructField("airport_fee", DoubleType, true)
    ))

    def normalizeSchema(df: DataFrame): DataFrame = {
      var normalized = df
      
      // Handle airport_fee vs Airport_fee column name variation
      if (df.columns.contains("Airport_fee") && !df.columns.contains("airport_fee")) {
        normalized = normalized.withColumnRenamed("Airport_fee", "airport_fee")
      }
      
      // Cast all columns to target types
      targetSchema.fields.foreach { field =>
        if (normalized.columns.contains(field.name)) {
          normalized = normalized.withColumn(field.name, F.col(field.name).cast(field.dataType))
        } else {
          // Add missing column as null
          normalized = normalized.withColumn(field.name, F.lit(null).cast(field.dataType))
        }
      }
      
      // Select only the columns we want in the correct order
      normalized.select(targetSchema.fieldNames.map(F.col): _*)
    }

    // Read and normalize each file, then union them
    val dataFrames = (1 to 12).map { month =>
      val monthStr = f"$month%02d"
      val path = s"s3a://$bucket/$year/yellow_tripdata_$year-$monthStr.parquet"
      
      println(s"Reading: $path")
      val df = spark.read.parquet(path)
      val normalized = normalizeSchema(df)

      println(s"Raw data count: ${df.count()}")

      val dfValidated = normalized 
        .filter(F.col("passenger_count") >= 0)
        .filter(F.col("trip_distance") >= 0)
        .filter(F.col("fare_amount") >= 0)
        .filter(F.col("extra") >= 0)
        .filter(F.col("mta_tax") >= 0)
        .filter(F.col("tip_amount") >= 0)
        .filter(F.col("tolls_amount") >= 0)
        .filter(F.col("improvement_surcharge") >= 0)
        .filter(F.col("total_amount") >= 0)
        .filter(F.col("congestion_surcharge") >= 0)
        .filter(F.col("airport_fee") >= 0)
        .filter(F.col("VendorID").isin(vendorIDs:_*))
        .filter(F.col("RatecodeID").isin(rateCodes:_*))
        .filter(F.col("store_and_fwd_flag").isin(storeAndFwdFlags:_*))
        .filter(F.col("payment_type").isin(paymentTypes:_*))
        .filter(F.col("tpep_dropoff_datetime") >= F.col("tpep_pickup_datetime"))

      println(s"Validated data count: ${dfValidated.count()}")

      val outputPath = s"s3a://$validatedBucket/$year/"
      dfValidated.write
        .mode("append")
        .parquet(outputPath)

      println(s"Validated Parquet written to $outputPath")

    }

    // Union all dataframes
    //val dfRaw = dataFrames.reduce(_ union _)

    
  }
}
