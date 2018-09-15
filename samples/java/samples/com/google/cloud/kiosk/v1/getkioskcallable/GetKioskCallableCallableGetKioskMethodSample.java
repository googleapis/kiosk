//// [ This is an auto-generated sample file produced by the gapic-generator. Sample name: "GetKioskCallableCallableGetKioskMethodSample" ]
//// STUB standalone sample "GetKioskCallableCallableGetKioskMethodSample" /////

// FIXME: Insert here set-up comments that we never want to display in cloudsite. These are seen by users perusing the samples directly in the repository.

// [START sample]

// FIXME: Insert here boilerplate code not directly related to the method call itself.

//      calling form: "Callable"
//        region tag: "sample"
//         className: "GetKioskCallableCallableGetKioskMethodSample"
//          valueSet: "get_kiosk_method_sample" ("Get Kiosk Method Sample")
//       description: "Get Kiosk Method Sample"
//        [id=1024]
//      apiMethod "getKioskCallable" of type "CallableMethod"

// FIXME: Insert here code to prepare the request fields, make the call, process the response.

public class GetKioskCallableCallableGetKioskMethodSample {
  public static void main(String[] args) {
    // [START sample_core]
    int id = 1024;
    GetKioskRequest request = GetKioskRequest.newBuilder().setId(id).build();
    ApiFuture<Kiosk> future = displayClient.getKioskCallable().futureCall(request);

    // Do something

    Kiosk response = future.get();
    Kiosk kiosk = response;
    System.out.printf("Response: %s\n", kiosk);
    // [END sample_core]
  }
}

// FIXME: Insert here clean-up code.

// [END sample]
