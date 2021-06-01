package org.hyperledger.bpa.impl.aries;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationRequest;
import org.hyperledger.aries.api.present_proof.PresentationRequestCredentials;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.exception.PresentationConstructionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class PresentationRequestHelperTest {

    private Gson gson = GsonConfig.defaultConfig();

    @Test
    void testSingleCredential() {
        PresentationRequestCredentials cred = gson.fromJson(requestCredentialsSingle,
                PresentationRequestCredentials.class);
        PresentationExchangeRecord ex = gson.fromJson(presExSingle, PresentationExchangeRecord.class);

        Optional<PresentationRequest> presentationRequest = PresentationRequestHelper.buildAny(ex, List.of(cred));
        Assertions.assertTrue(presentationRequest.isPresent());
    }

    @Test
    void testMultipleCredentials() {
        List<PresentationRequestCredentials> cred = gson.fromJson(requestCredentialsMulti,
                new TypeToken<Collection<PresentationRequestCredentials>>() {
                }.getType());
        PresentationExchangeRecord ex = gson.fromJson(presExMulti, PresentationExchangeRecord.class);

        Optional<PresentationRequest> presentationRequest = PresentationRequestHelper.buildAny(ex, cred);
        Assertions.assertTrue(presentationRequest.isPresent());
    }

    @Test
    void testNoMatchingCredentialFound() {
        PresentationRequestCredentials cred = gson.fromJson(requestCredentialsSingle,
                                PresentationRequestCredentials.class);
        PresentationExchangeRecord ex = gson.fromJson(presExMulti, PresentationExchangeRecord.class);
        Optional<PresentationRequest> presentationRequest = Optional.empty();
        try {
            presentationRequest = PresentationRequestHelper.buildAny(ex, List.of(cred));
        } catch (PresentationConstructionException e){
            Assertions.assertTrue(true);
        }
        if (presentationRequest.isPresent()){
            throw new AssertionError("Expected exception not throw");
        }
    }

    private String requestCredentialsSingle = "  {\n" +
            "    \"cred_info\": {\n" +
            "      \"referent\": \"ef25ac99-372e-4076-af47-19ce6cee4579\",\n" +
            "      \"attrs\": {\n" +
            "        \"iban\": \"1234\",\n" +
            "        \"bic\": \"4321\"\n" +
            "      },\n" +
            "      \"schema_id\": \"M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0\",\n" +
            "      \"cred_def_id\": \"M6Mbe3qx7vB4wpZF4sBRjt:3:CL:571:Bank Account V2\",\n" +
            "      \"rev_reg_id\": null,\n" +
            "      \"cred_rev_id\": null\n" +
            "    },\n" +
            "    \"interval\": null,\n" +
            "    \"presentation_referents\": [\n" +
            "      \"attribute_group_0\"\n" +
            "    ]\n" +
            "  }";

    private String presExSingle = "    {\n" +
            "      \"presentation_request\": {\n" +
            "        \"name\": \"Proof request\",\n" +
            "        \"version\": \"1.0\",\n" +
            "        \"requested_attributes\": {\n" +
            "          \"attribute_group_0\": {\n" +
            "            \"names\": [\n" +
            "              \"bic\",\n" +
            "              \"iban\"\n" +
            "            ],\n" +
            "            \"restrictions\": [\n" +
            "              {\n" +
            "                \"schema_id\": \"M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0\"\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        },\n" +
            "        \"requested_predicates\": {},\n" +
            "        \"nonce\": \"1046094633050809199221859\"\n" +
            "      },\n" +
            "      \"role\": \"prover\",\n" +
            "      \"created_at\": \"2021-05-31 15:09:24.300452Z\",\n" +
            "      \"connection_id\": \"906db6b6-9226-4e57-8aae-3d15b6a57fd9\",\n" +
            "      \"initiator\": \"external\",\n" +
            "      \"auto_present\": true,\n" +
            "      \"presentation\": {\n" +
            "        \"proof\": {\n" +
            "          \"proofs\": [\n" +
            "            {\n" +
            "              \"primary_proof\": {\n" +
            "                \"eq_proof\": {\n" +
            "                  \"revealed_attrs\": {\n" +
            "                    \"bic\": \"4321\",\n" +
            "                    \"iban\": \"1234\"\n" +
            "                  },\n" +
            "                  \"a_prime\": \"69287143185935528678703885844864089255435869703623859423852408408644126096861608214510586940297388151405293255539974848956471822389301868318230743388691272117926925086549873556951429926227125876738383257948118503202241688538383956538028922822303852444312398716405292829371942386088588904709756326744856442933283542353652180408549014211019757876485151491826454781611661931479219318877024867794237640044094395473998786971181407902521764368591784732121331105321139979550521268895959362366396553232953088460349642659984930696685747094292739362710733879860742856027073878395751393342822538142574585572590257564833059747677\",\n"
            +
            "                  \"e\": \"59315837647278016418099732808021164304337758059715143389584799318732172249092639667526076923773687465915277485402114254386641302484092631\",\n"
            +
            "                  \"v\": \"351777423744752184067118337479397674115993782961650197780306138390012561916066607411821213763442049593339372844955344552982303164732792966268645634856689491027382275486422572777408443614878926367853150677926509622073110942306780698650924506138160238478464754403532806502683986594833231963992054321736869367441156859075330029603273452114156035039677185176525913739248664965621848208396630703719196565993222390238089396106066868482745388681757876100523593742333741377565888123320049052147512092596946675375298839136326447246128350543607958624997846422864227539553000192493067764672442346944343786555460388230910189625041953561546640946516385276511778846218741518820436663430301357610933977022093192399681569941043426099799901169525152219783716814626309200518716191208683428112160684101068323765274585630766280791753193440960570618263334296843441865376126933456603165199303223964114158700819228370583501105374750638648684551\",\n"
            +
            "                  \"m\": {\n" +
            "                    \"master_secret\": \"7373990920239517754705356681552988940004536865271332402768390251842604326463669903805719289840924598652584779470223947688761358303026461595775313933155645674327509456896853858185\"\n"
            +
            "                  },\n" +
            "                  \"m2\": \"14568456272944263674615453526956064296866399765535254359159125723123661112054120424796702530915900348597269918995253093144474334695894353262393037164358816568659875483979633266378\"\n"
            +
            "                },\n" +
            "                \"ge_proofs\": []\n" +
            "              },\n" +
            "              \"non_revoc_proof\": null\n" +
            "            }\n" +
            "          ],\n" +
            "          \"aggregated_proof\": {\n" +
            "            \"c_hash\": \"52259661752404166696502468186481736513652029083145854484045997437778578048384\",\n"
            +
            "            \"c_list\": []\n" +
            "          }\n" +
            "        },\n" +
            "        \"requested_proof\": {\n" +
            "          \"revealed_attrs\": {},\n" +
            "          \"revealed_attr_groups\": {\n" +
            "            \"attribute_group_0\": {\n" +
            "              \"sub_proof_index\": 0,\n" +
            "              \"values\": {\n" +
            "                \"bic\": {\n" +
            "                  \"raw\": \"4321\",\n" +
            "                  \"encoded\": \"4321\"\n" +
            "                },\n" +
            "                \"iban\": {\n" +
            "                  \"raw\": \"1234\",\n" +
            "                  \"encoded\": \"1234\"\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          },\n" +
            "          \"self_attested_attrs\": {},\n" +
            "          \"unrevealed_attrs\": {},\n" +
            "          \"predicates\": {}\n" +
            "        },\n" +
            "        \"identifiers\": [\n" +
            "          {\n" +
            "            \"schema_id\": \"M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0\",\n" +
            "            \"cred_def_id\": \"M6Mbe3qx7vB4wpZF4sBRjt:3:CL:571:Bank Account V2\",\n" +
            "            \"rev_reg_id\": null,\n" +
            "            \"timestamp\": null\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      \"presentation_request_dict\": {\n" +
            "        \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/present-proof/1.0/request-presentation\",\n" +
            "        \"@id\": \"689e2ab1-4168-446a-902f-81ceaec16412\",\n" +
            "        \"request_presentations~attach\": [\n" +
            "          {\n" +
            "            \"@id\": \"libindy-request-presentation-0\",\n" +
            "            \"mime-type\": \"application/json\",\n" +
            "            \"data\": {\n" +
            "              \"base64\": \"eyJuYW1lIjogIlByb29mIHJlcXVlc3QiLCAidmVyc2lvbiI6ICIxLjAiLCAicmVxdWVzdGVkX2F0dHJpYnV0ZXMiOiB7ImF0dHJpYnV0ZV9ncm91cF8wIjogeyJuYW1lcyI6IFsiYmljIiwgImliYW4iXSwgInJlc3RyaWN0aW9ucyI6IFt7InNjaGVtYV9pZCI6ICJNNk1iZTNxeDd2QjR3cFpGNHNCUmp0OjI6YmFua19hY2NvdW50OjEuMCJ9XX19LCAicmVxdWVzdGVkX3ByZWRpY2F0ZXMiOiB7fSwgIm5vbmNlIjogIjEwNDYwOTQ2MzMwNTA4MDkxOTkyMjE4NTkifQ==\"\n"
            +
            "            }\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      \"updated_at\": \"2021-05-31 15:09:24.971834Z\",\n" +
            "      \"trace\": false,\n" +
            "      \"presentation_exchange_id\": \"9bdba6ce-115e-45ab-8b10-7ef1ab1003ed\",\n" +
            "      \"state\": \"presentation_acked\",\n" +
            "      \"thread_id\": \"689e2ab1-4168-446a-902f-81ceaec16412\"\n" +
            "    }";

    private String requestCredentialsMulti = "[\n" +
            "  {\n" +
            "    \"cred_info\": {\n" +
            "      \"referent\": \"6348003e-e7c4-4ace-a9ce-f65fabf4d810\",\n" +
            "      \"attrs\": {\n" +
            "        \"dateOfBirth\": \"mmm\",\n" +
            "        \"dateOfExpiry\": \"lmlmmlM\",\n" +
            "        \"nationality\": \"mmm\",\n" +
            "        \"academicTitle\": \"mmm\",\n" +
            "        \"addressZipCode\": \"kklln\",\n" +
            "        \"documentType\": \"mmmm\",\n" +
            "        \"familyName\": \"mmm\",\n" +
            "        \"addressCity\": \"mm\",\n" +
            "        \"birthName\": \"mmm\",\n" +
            "        \"addressStreet\": \"mmm\",\n" +
            "        \"placeOfBirth\": \"mmm\",\n" +
            "        \"firstName\": \"mmm\",\n" +
            "        \"addressCountry\": \"mmllm\"\n" +
            "      },\n" +
            "      \"schema_id\": \"847fVkFJiNZ4FUew9g6Zn4:2:Basis-ID:1.0\",\n" +
            "      \"cred_def_id\": \"VoSfM3eGaPxduty34ySygw:3:CL:2899:Basis-ID-Alice\",\n" +
            "      \"rev_reg_id\": null,\n" +
            "      \"cred_rev_id\": null\n" +
            "    },\n" +
            "    \"interval\": null,\n" +
            "    \"presentation_referents\": [\n" +
            "      \"masterId\"\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"cred_info\": {\n" +
            "      \"referent\": \"ef25ac99-372e-4076-af47-19ce6cee4579\",\n" +
            "      \"attrs\": {\n" +
            "        \"bic\": \"4321\",\n" +
            "        \"iban\": \"1234\"\n" +
            "      },\n" +
            "      \"schema_id\": \"M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0\",\n" +
            "      \"cred_def_id\": \"M6Mbe3qx7vB4wpZF4sBRjt:3:CL:571:Bank Account V2\",\n" +
            "      \"rev_reg_id\": null,\n" +
            "      \"cred_rev_id\": null\n" +
            "    },\n" +
            "    \"interval\": null,\n" +
            "    \"presentation_referents\": [\n" +
            "      \"bank_account\"\n" +
            "    ]\n" +
            "  }\n" +
            "]";

    private String presExMulti = "{\n" +
            "    \"trace\": false,\n" +
            "    \"connection_id\": \"8bc1ccef-7ae3-40e9-90bf-12d3228856d3\",\n" +
            "    \"thread_id\": \"c1695bbc-7fed-4e49-b848-67f56f31b6c5\",\n" +
            "    \"role\": \"prover\",\n" +
            "    \"initiator\": \"external\",\n" +
            "    \"presentation_request_dict\": {\n" +
            "        \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/present-proof/1.0/request-presentation\",\n" +
            "        \"@id\": \"c1695bbc-7fed-4e49-b848-67f56f31b6c5\",\n" +
            "        \"comment\": \"string\",\n" +
            "        \"request_presentations~attach\": [\n" +
            "            {\n" +
            "                \"@id\": \"libindy-request-presentation-0\",\n" +
            "                \"mime-type\": \"application/json\",\n" +
            "                \"data\": {\n" +
            "                    \"base64\": \"eyJuYW1lIjogIlByb29mIHJlcXVlc3QiLCAidmVyc2lvbiI6ICIxLjAiLCAicmVxdWVzdGVkX2F0dHJpYnV0ZXMiOiB7ImJhbmtfYWNjb3VudCI6IHsibmFtZXMiOiBbImliYW4iXSwgInJlc3RyaWN0aW9ucyI6IFt7InNjaGVtYV9pZCI6ICJNNk1iZTNxeDd2QjR3cFpGNHNCUmp0OjI6YmFua19hY2NvdW50OjEuMCJ9XX0sICJtYXN0ZXJJZCI6IHsibmFtZXMiOiBbImZpcnN0TmFtZSJdLCAicmVzdHJpY3Rpb25zIjogW3sic2NoZW1hX2lkIjogIjg0N2ZWa0ZKaU5aNEZVZXc5ZzZabjQ6MjpCYXNpcy1JRDoxLjAifV19fSwgInJlcXVlc3RlZF9wcmVkaWNhdGVzIjoge30sICJub25jZSI6ICI1NTc5ODc2MzA1OTcwNTIwNTI1NDgwOTYifQ==\"\n"
            +
            "                }\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "    \"presentation_exchange_id\": \"2b72e261-b7c1-4ae9-832a-991aa10d9c3f\",\n" +
            "    \"created_at\": \"2021-06-01 15:53:47.789604Z\",\n" +
            "    \"updated_at\": \"2021-06-01 15:53:47.789604Z\",\n" +
            "    \"state\": \"request_received\",\n" +
            "    \"presentation_request\": {\n" +
            "        \"name\": \"Proof request\",\n" +
            "        \"version\": \"1.0\",\n" +
            "        \"requested_attributes\": {\n" +
            "            \"bank_account\": {\n" +
            "                \"names\": [\n" +
            "                    \"iban\"\n" +
            "                ],\n" +
            "                \"restrictions\": [\n" +
            "                    {\n" +
            "                        \"schema_id\": \"M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"masterId\": {\n" +
            "                \"names\": [\n" +
            "                    \"firstName\"\n" +
            "                ],\n" +
            "                \"restrictions\": [\n" +
            "                    {\n" +
            "                        \"schema_id\": \"847fVkFJiNZ4FUew9g6Zn4:2:Basis-ID:1.0\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        },\n" +
            "        \"requested_predicates\": {},\n" +
            "        \"nonce\": \"557987630597052052548096\"\n" +
            "    }\n" +
            "}";
}