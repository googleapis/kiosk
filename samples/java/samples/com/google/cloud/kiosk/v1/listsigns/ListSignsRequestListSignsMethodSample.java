//// [ This is an auto-generated sample file produced by the gapic-generator. Sample name: "ListSignsRequestListSignsMethodSample" ]
//// STUB standalone sample "ListSignsRequestListSignsMethodSample" /////

// FIXME: Insert here set-up comments that we never want to display in cloudsite. These are seen by users perusing the samples directly in the repository.

// [START sample]

// FIXME: Insert here boilerplate code not directly related to the method call itself.

//      calling form: "Request"
//        region tag: "sample"
//         className: "ListSignsRequestListSignsMethodSample"
//          valueSet: "list_signs_method_sample" ("List Signs Method Sample")
//       description: "List Signs Method Sample"
//        []
//      apiMethod "listSigns" of type "RequestObjectMethod"

// FIXME: Insert here code to prepare the request fields, make the call, process the response.

public class ListSignsRequestListSignsMethodSample {
  public static void main(String[] args) {
    // [START sample_core]
    Empty request = Empty.newBuilder().build();
    ListSignsResponse response = displayClient.listSigns(request);
    for (Sign sign : response.getSignsList()) {
      System.out.printf("Sign: %s\n", sign);
    }
    // [END sample_core]
  }
}

// FIXME: Insert here clean-up code.

// [END sample]
