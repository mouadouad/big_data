import org.apache.spark.sql.SparkSession

object Main extends App{

  val spark = SparkSession.builder()
    .appName("NYC Taxi Validation")
    .master("local[*]")
    .config("fs.s3a.access.key", "minio")
    .config("fs.s3a.secret.key", "minio123")
    .config("fs.s3a.endpoint", "http://localhost:9000/")
    .config("fs.s3a.path.style.access", "true")
    .config("fs.s3a.connection.ssl.enable", "false")
    .config("fs.s3a.attempts.maximum", "1")
    .config("fs.s3a.connection.establish.timeout", "6000")
    .config("fs.s3a.connection.timeout", "5000")
    .getOrCreate()
  spark.sparkContext.setLogLevel("WARN")

  println("=" * 60)
  println("NYC Taxi Data Ingestion - Exercise 2")
  println("=" * 60)

  println("\n[1/3] Validating data and writing to validated bucket...")
  Validator.validate(spark)

  val jdbcUrl = "jdbc:postgresql://localhost:5432/taxi"
  val jdbcProps = new java.util.Properties()
  jdbcProps.setProperty("user", "postgres")
  jdbcProps.setProperty("password", "postgres")
  jdbcProps.setProperty("driver", "org.postgresql.Driver")

  // Skipping zone lookup copy - already populated in database (265 rows)
  // println("\n[2/3] Copying taxi zone lookup to PostgreSQL...")
  // Ingestor.copyLookUp(spark, jdbcUrl, jdbcProps)

  println("\n[2/2] Ingesting validated data to PostgreSQL...")
  Ingestor.ingestData(spark, jdbcUrl, jdbcProps)

  println("\n" + "=" * 60)
  println("✓ Data ingestion complete!")
  println("=" * 60)

  spark.stop()
}
