import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;
import org.junit.Test;
import com.jayway.restassured.response.Response;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import static com.jayway.awaitility.Awaitility.await;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class fileApiTest {

  private static String fileId;
  private String fileName = "APPLE_EX";
  //Ideally Token should have been extracted through OAuth,
  // since it was not mentioned, value has been hard-coded.
  final String TOKEN = "xoxp-30829859861-30819630308-30829897184-a5b1f9cd2c";
  private final String URL = "https://slack.com/api/";

  final ConditionFactory WAIT = await().atMost(new Duration(45, TimeUnit.SECONDS))
    .pollInterval(Duration.FIVE_SECONDS)
    .pollDelay(Duration.FIVE_SECONDS);

  private Response post(String fileName) {
    Response response = given().
      param("token", TOKEN).
      multiPart(new File("src/main/resources/" + fileName + ".png")).
      when().
      post(URL + "files.upload").
      then().
      statusCode(200).extract().response();
    return response;
  }

  private Response get() {
    Response response = given().param("token", TOKEN).
      param("types", "images").
      when().
      get(URL + "files.list").
      then().statusCode(200).extract().response();
    return response;
  }

  private Response delete(String fileId) {
    Response response = given().param("token", TOKEN).
      param("file", fileId).
      when().
      delete(URL + "files.delete").
      then().statusCode(200).extract().response();
    return response;
  }

  /**
   * files.upload
   1. Upload a PNG file no greater than 1 MB in size
   2. files.upload returns a file object with a file ID and all expected
   thumbnail URLs
   3. The thumbnail URLs point to what appear to be the correct file –
   the filename will be a lowercase version of the original upload
   */
  @Test
  public void test1FileUploadTest() {

    Response response = post(fileName);

    boolean ok = response.path("ok");
    fileId = response.path("file.id");
    String thumb_64_URL = response.path("file.thumb_64");
    String thumb_80_URL = response.path("file.thumb_80");
    String thumb_360_URL = response.path("file.thumb_360");
    String thumb_160_URL = response.path("file.thumb_160");

    //Assert value of ok
    assertThat("Ok is not true", ok == true);
    //Empty Check for ID
    assertThat("File ID is empty", !fileId.isEmpty());
    //Null Check  for ID
    assertThat("File ID is null", !fileId.equals(null));

    //Thumbnail 64
    assertThat("Thumbnail is empty ", !thumb_64_URL.isEmpty());
    assertThat("thumb_64 is null", !thumb_64_URL.equals(null));
    assertThat("fileName is not in lowercase", thumb_64_URL.contains(fileName.toLowerCase()));
    //Thumbnail 80
    assertThat("Thumbnail is empty ", !thumb_80_URL.isEmpty());
    assertThat("thumb_80 is null", !thumb_80_URL.equals(null));
    assertThat("fileName is not in lowercase", thumb_80_URL.contains(fileName.toLowerCase()));
    //Thumbnail 360
    assertThat("Thumbnail is empty ", !thumb_360_URL.isEmpty());
    assertThat("thumb_360 is null", !thumb_360_URL.equals(null));
    assertThat("fileName is not in lowercase", thumb_360_URL.contains(fileName.toLowerCase()));

    //Thumbnail 160
    assertThat("Thumbnail is empty ", !thumb_160_URL.isEmpty());
    assertThat("thumb_160 is null", !thumb_160_URL.equals(null));
    assertThat("fileName is not in lowercase", thumb_160_URL.contains(fileName.toLowerCase()));

  }

  /**
   * files.list
   A file you uploaded is properly listed in the response with the correct ID
   */
  @Test
  public void test2FileList() {
    /*File doesn't appear instantly, have to poll (explicit wait).
    Polling every 5 seconds upto 45 seconds
    polling stops if file appears anytime before 45 seconds*/

    WAIT.until(new Callable<String>() {
      public String call() throws Exception {
        return get().path("files[0].id");
      }
    }, equalTo(fileId));

  }


  /**
   * List only by type:images when calling the endpoint
   */
  @Test
  public void test3FileList() {
    // Asserting only png files are shown,
    // since other fileName are unknown to me.
    Response response = get();

    List<String> fileType = response.path("files.filetype");

    for (String s : fileType) {
      assertThat("Contains files other than png", s.equals("png"));
    }
  }

  /**
   * files.delete
   Delete a file you uploaded and confirm it is deleted
   */
  @Test
  public void test4DeleteUploadedFile() {
    Response response = delete(fileId);

    assertThat("Error deleting file", response.path("ok").equals(true));

    //confirm file is deleted by doing a get
    WAIT.until(new Callable<String>() {
      public String call() throws Exception {
        return get().path("files[0].id");
      }
    }, not(equalTo(fileId)));

    //deleting the file again to confirm
    Response response2 = delete(fileId);

    assertThat("Error should say file deleted", response2.path("error").equals("file_deleted"));
  }

  /**
   * Another test is to try deleting
   * a file that doesn't exist and
   * confirm that the correct error message appears
   */
  @Test
  public void test5DeleteNonExistentFile() {
    Response response = delete("123ASDFG");

    assertThat("should give ok as false", response.path("ok").equals(false));
    assertThat("should give an error", response.path("error").equals("file_not_found"));
  }

}