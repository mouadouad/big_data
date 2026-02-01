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

  Validator.validate(spark)

  val jdbcUrl = "jdbc:postgresql://localhost:5432/taxi"
  val jdbcProps = new java.util.Properties()
  jdbcProps.setProperty("user", "postgres")
  jdbcProps.setProperty("password", "postgres")
  jdbcProps.setProperty("driver", "org.postgresql.Driver")


  Ingestor.copyLookUp(spark, jdbcUrl, jdbcProps)
  Ingestor.ingestData(spark, jdbcUrl, jdbcProps)

  spark.stop()
  }
