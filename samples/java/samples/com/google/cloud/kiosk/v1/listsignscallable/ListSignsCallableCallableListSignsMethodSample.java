//// [ This is an auto-generated sample file produced by the gapic-generator. Sample name: "ListSignsCallableCallableListSignsMethodSample" ]
//// STUB standalone sample "ListSignsCallableCallableListSignsMethodSample" /////

// FIXME: Insert here set-up comments that we never want to display in cloudsite. These are seen by users perusing the samples directly in the repository.

// [START sample]

// FIXME: Insert here boilerplate code not directly related to the method call itself.

//      calling form: "Callable"
//        region tag: "sample"
//         className: "ListSignsCallableCallableListSignsMethodSample"
//          valueSet: "list_signs_method_sample" ("List Signs Method Sample")
//       description: "List Signs Method Sample"
//        []
//      apiMethod "listSignsCallable" of type "CallableMethod"

// FIXME: Insert here code to prepare the request fields, make the call, process the response.

public class ListSignsCallableCallableListSignsMethodSample {
  public static void main(String[] args) {
    // [START sample_core]
    Empty request = Empty.newBuilder().build();
    ApiFuture<ListSignsResponse> future = displayClient.listSignsCallable().futureCall(request);

    // Do something

    ListSignsResponse response = future.get();
    for (Sign sign : response.getSignsList()) {
      System.out.printf("Sign: %s\n", sign);
    }
    // [END sample_core]
  }
}

// FIXME: Insert here clean-up code.

// [END sample]
