//// [ This is an auto-generated sample file produced by the gapic-generator. Sample name: "GetKioskRequestGetKioskMethodSample" ]
//// STUB standalone sample "GetKioskRequestGetKioskMethodSample" /////

// FIXME: Insert here set-up comments that we never want to display in cloudsite. These are seen by users perusing the samples directly in the repository.

// [START sample]

// FIXME: Insert here boilerplate code not directly related to the method call itself.

//      calling form: "Request"
//        region tag: "sample"
//         className: "GetKioskRequestGetKioskMethodSample"
//          valueSet: "get_kiosk_method_sample" ("Get Kiosk Method Sample")
//       description: "Get Kiosk Method Sample"
//        [id=1024]
//      apiMethod "getKiosk" of type "RequestObjectMethod"

// FIXME: Insert here code to prepare the request fields, make the call, process the response.

public class GetKioskRequestGetKioskMethodSample {
  public static void main(String[] args) {
    // [START sample_core]
    int id = 1024;
    GetKioskRequest request = GetKioskRequest.newBuilder().setId(id).build();
    Kiosk response = displayClient.getKiosk(request);
    Kiosk kiosk = response;
    System.out.printf("Response: %s\n", kiosk);
    // [END sample_core]
  }
}

// FIXME: Insert here clean-up code.

// [END sample]
