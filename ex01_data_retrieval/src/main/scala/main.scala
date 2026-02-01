import io.minio._
import io.minio.PutObjectArgs
import java.net.URL

object UploadNYCTaxi extends App {

  val minioClient = MinioClient.builder()
    .endpoint("http://localhost:9000")
    .credentials("minio", "minio123")
    .build()

  val bucket = "nyc-raw"

  def uploadYearToMinio(year: Int, bucket: String, minioClient: io.minio.MinioClient): Unit = {

    for (month <- 1 to 12) {

      val monthStr = f"$month%02d"

      val objectName = s"$year/yellow_tripdata_$year-$monthStr.parquet"

      val parquetUrl =
        s"https://d37ci6vzurychx.cloudfront.net/trip-data/yellow_tripdata_$year-$monthStr.parquet"

      val inputStream = new URL(parquetUrl).openStream()

      minioClient.putObject(
        PutObjectArgs.builder()
          .bucket(bucket)
          .`object`(objectName)
          .stream(inputStream, -1, 10 * 1024 * 1024)
          .contentType("application/octet-stream")
          .build()
      )

      inputStream.close()
    }
  }

  uploadYearToMinio(2023, bucket, minioClient)
}


