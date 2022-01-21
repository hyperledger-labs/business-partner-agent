/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.bpa.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.hyperledger.bpa.controller.api.invitation.CheckInvitationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class InvitationParserTest {

    public static MockWebServer mockWebServer;
    private InvitationParser p;

    @BeforeEach
    void init() throws Exception {
        p = new InvitationParser();
        p.setMapper(new ObjectMapper());

        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void TestParseReceiveInvitation() {
        InvitationParser.Invitation invitation = p.parseInvitation(this.invitation);
        Assertions.assertFalse(invitation.isOob());
        Assertions.assertTrue(invitation.isParsed());
        Assertions.assertNotNull(invitation.getInvitation());
    }

    @Test
    void TestParseReceiveOOBInvitation() {
        InvitationParser.Invitation invitation = p.parseInvitation(oob);
        Assertions.assertTrue(invitation.isOob());
        Assertions.assertTrue(invitation.isParsed());
        Assertions.assertNotNull(invitation.getInvitation());
    }

    @Test
    void TestParseReceiveDidCommInvitation() {
        InvitationParser.Invitation invitation = p.parseInvitation(this.didCommInvitation);
        Assertions.assertFalse(invitation.isOob());
        Assertions.assertTrue(invitation.isParsed());
        Assertions.assertNotNull(invitation.getInvitation());
    }

    @Test
    void TestParseReceiveOOBDidCommInvitation() {
        InvitationParser.Invitation invitation = p.parseInvitation(didCommOob);
        Assertions.assertTrue(invitation.isOob());
        Assertions.assertTrue(invitation.isParsed());
        Assertions.assertNotNull(invitation.getInvitation());
    }

    @Test
    void testStreetcredURIWithRedirect() {
        MockResponse response = new MockResponse()
                .setResponseCode(301)
                .setHeader("location", streetCredRedirect);
        mockWebServer.enqueue(response);
        String httpUrl = mockWebServer.url("/46yG3VegpCqc").toString();
        CheckInvitationResponse parsed = p.checkInvitation(httpUrl);
        Assertions.assertEquals("Snapper", parsed.getLabel());
    }

    @Test
    void testStreetcredURI() {
        CheckInvitationResponse parsed = p.checkInvitation(
                URLEncoder.encode(streetCredRedirect, StandardCharsets.UTF_8));
        Assertions.assertEquals("Snapper", parsed.getLabel());
    }

    @Test
    void testOOBInvitationWithAttachedCredentialOfferWithRedirect() {
        MockResponse response = new MockResponse()
                .setResponseCode(301)
                .setHeader("location", oobInvitationWithAttachment);
        mockWebServer.enqueue(response);
        String httpUrl = mockWebServer.url("/" + UUID.randomUUID()).toString();
        CheckInvitationResponse parsed = p.checkInvitation(httpUrl);
        Assertions.assertEquals("oscar", parsed.getLabel());
        Assertions.assertNotNull(parsed.getInvitationBlock());
    }

    @Test
    void testOOBInvitationWithAttachedCredentialOffer() {
        CheckInvitationResponse parsed = p.checkInvitation(
                URLEncoder.encode(oobInvitationWithAttachment, StandardCharsets.UTF_8));
        Assertions.assertEquals("oscar", parsed.getLabel());
        Assertions.assertNotNull(parsed.getInvitationBlock());
    }

    private final String invitation = "ewogICAgIkB0eXBlIjogImRpZDpzb3Y6QnpDYnNOWWhNcmpIaXFaRFRVQVNIZztzcGVjL2Nvbm5lY3Rpb25zLzEuMC9pbnZpdGF0aW9uIiwKICAgICJAaWQiOiAiNGQ1OGJhZjktZDIwOS00MTE4LThkOTQtNGE0OTBlNGEwNGFhIiwKICAgICJzZXJ2aWNlRW5kcG9pbnQiOiAiaHR0cDovL2hvc3QuZG9ja2VyLmludGVybmFsOjgwMzAiLAogICAgInJlY2lwaWVudEtleXMiOiBbCiAgICAgICAgIjZCTlF1dFJIalNWNmJwQ0E2djVkRVB2NW12dWlRS2hyc256cEN4dUgzdXdqIgogICAgXSwKICAgICJsYWJlbCI6ICJCdXNpbmVzcyBQYXJ0bmVyIEFnZW50IDEiCn0=";
    private final String oob = "eyJAdHlwZSI6ICJkaWQ6c292OkJ6Q2JzTlloTXJqSGlxWkRUVUFTSGc7c3BlYy9vdXQtb2YtYmFuZC8xLjAvaW52aXRhdGlvbiIsICJAaWQiOiAiMmZhYmJhNzYtZTlhNy00Yzk4LTg2ZjMtMTFkNGE1MTYzYjQyIiwgImhhbmRzaGFrZV9wcm90b2NvbHMiOiBbImRpZDpzb3Y6QnpDYnNOWWhNcmpIaXFaRFRVQVNIZztzcGVjL2RpZGV4Y2hhbmdlLzEuMCJdLCAic2VydmljZXMiOiBbImRpZDpzb3Y6RXJhWUNESlVQc0NoYmt3N1MxdlY5NiJdLCAibGFiZWwiOiAiYm9iIn0=";

    private final String didCommInvitation = "eyJAdHlwZSI6ICJodHRwczovL2RpZGNvbW0ub3JnL2Nvbm5lY3Rpb25zLzEuMC9pbnZpdGF0aW9uIiwgIkBpZCI6ICJkNGE5ZmY4YS1jNjlmLTRiMWQtODJlYi04NzQwYWRiMzE0MmEiLCAic2VydmljZUVuZHBvaW50IjogImh0dHBzOi8vaW52aXRlMS1icGEtYWNhcHktZGV2LmFwcHMuc2lsdmVyLmRldm9wcy5nb3YuYmMuY2EiLCAibGFiZWwiOiAiaW52aXRlMSIsICJyZWNpcGllbnRLZXlzIjogWyI5MnV2TTFFOG9RbXFUNGZLZkdtam5UTndiandqYUZXYVRpRmtMZXNvbnhSVCJdfQ==";
    private final String didCommOob = "eyJAdHlwZSI6ICJodHRwczovL2RpZGNvbW0ub3JnL291dC1vZi1iYW5kLzEuMC9pbnZpdGF0aW9uIiwgIkBpZCI6ICI2ZmYzY2UzNy1kYjM1LTRjYTctYTNkOS03MWJmNGYxYzhkYzQiLCAibGFiZWwiOiAiaW52aXRlMSIsICJzZXJ2aWNlcyI6IFsiZGlkOnNvdjpXc3FWaW4xWjRZdnZiODdzU1E3QzJtIl0sICJoYW5kc2hha2VfcHJvdG9jb2xzIjogWyJodHRwczovL2RpZGNvbW0ub3JnL2RpZGV4Y2hhbmdlLzEuMCJdfQ==";

    private final String streetCredRedirect = "id.streetcred://launch/?d_m=eyJsYWJlbCI6IlNuYXBwZXIiLCJpbWFnZVVybCI6bnVsbCwic2VydmljZUVuZHBvaW50IjoiaHR0cHM6Ly90cmluc2ljLW1lZGlhdG9yLWFnZW50LWV1cm9wZS5henVyZXdlYnNpdGVzLm5ldC8iLCJyb3V0aW5nS2V5cyI6WyJDTFBmc3hVaDNMOWR2U2huNjRmYkZKZExrbzZHbmVhQkNEWkJQNjZpWVV3RCJdLCJyZWNpcGllbnRLZXlzIjpbIkc5cDVydVRqcDJiVHhWellIUVpySmZISkNDaENRVUpOVllrUWhTcGlmWTdkIl0sIkBpZCI6IjBiNTc1Zjc4LTNiNTQtNGFhNS1hMzMyLTcwNTljZDg5YzA1NiIsIkB0eXBlIjoiZGlkOnNvdjpCekNic05ZaE1yakhpcVpEVFVBU0hnO3NwZWMvY29ubmVjdGlvbnMvMS4wL2ludml0YXRpb24ifQ%3D%3D&orig=https%3a%2f%2fredir.trinsic.id%2f46yG3VegpCqc";

    private final String oobInvitationWithAttachment = "didcomm://oscar.iil.network?oob=eyJAaWQiOiI1MTdjZTlkMy1hM2I1LTRkNjItYjQxNC0xNmRjZGRhMDhhNmUiLCJAdHlwZSI6ImRpZDpzb3Y6QnpDYnNOWWhNcmpIaXFaRFRVQVNIZztzcGVjL291dC1vZi1iYW5kLzEuMC9pbnZpdGF0aW9uIiwiaGFuZHNoYWtlX3Byb3RvY29scyI6WyJkaWQ6c292OkJ6Q2JzTlloTXJqSGlxWkRUVUFTSGc7c3BlYy9kaWRleGNoYW5nZS8xLjAiXSwibGFiZWwiOiJvc2NhciIsInJlcXVlc3RzfmF0dGFjaCI6W3siQGlkIjoicmVxdWVzdC0wIiwiZGF0YSI6eyJqc29uIjp7IkBpZCI6ImI3OTdhZDllLTk4YjUtNDA2Yi1iYzlmLWRiY2JiNmVmMGNlZCIsIkB0eXBlIjoiZGlkOnNvdjpCekNic05ZaE1yakhpcVpEVFVBU0hnO3NwZWMvaXNzdWUtY3JlZGVudGlhbC8xLjAvb2ZmZXItY3JlZGVudGlhbCIsIn50aHJlYWQiOnt9LCJvZmZlcnN+YXR0YWNoIjpbeyJAaWQiOiJsaWJpbmR5LWNyZWQtb2ZmZXItMCIsImRhdGEiOnsiYmFzZTY0IjoiZXlKelkyaGxiV0ZmYVdRaU9pQWlUVFpOWW1VemNYZzNka0kwZDNCYVJqUnpRbEpxZERveU9tSmhibXRmWVdOamIzVnVkRG94TGpBaUxDQWlZM0psWkY5a1pXWmZhV1FpT2lBaVZYWTFNM1phTVZOdVV6Tk9VRmxOVFZOeU5FSmhVVG96T2tOTU9qVTNNVHB2YzJOaGNpMWlZVzVyTFRBeElpd2dJbXRsZVY5amIzSnlaV04wYm1WemMxOXdjbTl2WmlJNklIc2lZeUk2SUNJMk9EWTRNems0TmpjNE9UQXhOVE00Tnpnd056QTRNREl6TnpReU9ETTNPRE00TlRJME1UUXhNakV5TXpjMk16WXpOek0zT1RVeE5UZzNORFUwTmprMU9UZ3dOREU0T1RFNE9UZzRJaXdnSW5oNlgyTmhjQ0k2SUNJeE5qSXpPVGM0TkRZNE9UUTBOREF3TWpFNU1qUTBOVE13TnprM016ZzJORE00TXpFeU16RTBNRFV3TURFMU1EWTRORGs1TURZM05UQTBNamsxTmpjME9UUXdOVEUyTXpNek9UazFOamt3TlRBMk5qZ3dNekF4TkRFMk56ZzNOREV3TURZd09UTTFOelkxT0RRM056VTJPRE00T1RFeU56SXlNamMwTWpFeE5Ua3pORGd4TURVeU16SXlPVFl6TWprMU56QTBOVGc0TmpBeE9ESTJNemc1TkRFNU5EazVNRFV6TWpFNE16RTRPVEV5TURReE1qYzVOVEk0TWprM016YzNNRGsyTURFeU5qa3dPRFU0TXpNM05UUTVNVGswTWpneU1EVXhOVEkwTkRReU5UazFORFUxTURVeU5qWXpNakV3TURrMU56QTFNalkxTnpJM05USTFNekUyTURBNE5EZ3dORE15TVRJd01ESXdOamMyT0RReE1EZ3dOREExT0RNNU5UQTFNVGc0TXpnNU56UTNOVFF4TnpFM09ETXdNelF3TXpjek1qQXpNVEkzTlRVM05UVTJPVFUwTmpJMk56WXlOek14T0RBM09EWXdNelUzTURBMU5qWTJNREE0TkRZM09EazVOVFkxTnpVNE9EWTFOek0xTWpFME56RTNPRFl3TmpBM016QTNOamcxTURnNU16TTRNemN6T1RNNU1Ea3hPVFk0TkRJME1qTXpOVGM0T0RjNE1ERTBNRFkxTnpBNU5qTXlOekl4TURNME1qY3pOREE0TWpZeU9URTVPVFk1TmpreE1qVTJNREU0T0RFNU1UYzFNemcwTmpVMk16RTRNRFExTWpjMk5EYzJNVEUwTlRVNE5qY3lNREF6TlRNd016QXpOemMxTmpRME1UUTVOVEF4T0RJMk5qa3dNamt4TWpFMk56UXlNalk1TWpFd056WTJNalkzTWpFM056STVNakEzTXpRNE9EWTRNemM0T0RrNU5EUXhPVFUzTnpJd05EVTVOamMwT1RnMU9UZ3pPVGM1TVRBeU1ESXlNRE14TWpFMU16Y3dORFkwTkRjMk5EazBNalEzTWpBek5EazRNekV6TnpJME56STFNVEV3TVRrMU5EUXlOVEV3T1RBd09EYzNNall5TXpRek5qVXlNVFV3TWpneU9EUTRORFUxTVRVMk16ZzBNREUzTlRnME16QXlPRE0yTnpZaUxDQWllSEpmWTJGd0lqb2dXMXNpWW1saklpd2dJamt6TkRNNE1qVTVNemMyTlRZNE1UazRNalF4TlRZME1USTJPVEUyTVRNMk1URXpNVEUwTVRRMU5qazVOakkwT1RZM09UTXpOREk1TWpNM01qVXpNelEyT0RrM05UY3pNak0zTmprd016VTNNekkxTURRNE1qZzBPRFU0T1RZMU56YzBNRGN5T1RZeE5EazROamN3TXpVNE56VTBOalEwT1RjeU9EWXdOakEwTlRNME5qWTJORGczTlRnNU1qWTNPVEEwTnpZNE5UVXhOelkxTnpBNE1qSTBOVE0xT0RFMk9EZzNNRE0xTmpFd09EWTFPRFkwTURjM05UUXdNelkzTWpRM016WTJPVFV6TVRVME9EWTVNVGczTXpNeU9EVXlOak0yTnpJeU9ERXdNVGszT0RJNU56RXpNVFl3TlRVd01UTXhOVFF4T0RVd05UVTVPVGt5T0RJNE56QXlPREU1TVRjMk16RTJPVFkwTkRBME5Ea3hPVEUxT0RrNE5qSTRNRFF5TWpNd05qWXpNRGc1TmpVME5EUTBNRFEzT1Rjd05qQTFNVFF4TkRjNE5EVXpNREUwT1RNMU16UTBOelU0TlRNeE56UTJOVGd6TlRVMk1UTTFPVEl5TnpReU1qSTROekU1TWpRd05UY3hPVE0xTlRFMU9ESTJNVFkyTVRJeE1EazNPREkwTnpNeU5UYzJPVFkwTURRNE9Ua3lOek0xTlRZNE5EWTBOekUzTlRnNE5qSXhOemt6TmprMU5UUXdNalEzTXpZeE56RTBNRFV5TVRrMU1qazBNVGt3TXpBMk56UTJNekU0TmpZME5qZ3hNelU1TVRjeE9UTTFORGN4TVRVM01EZzRNemt3TWpRek1UTTJPVE01T0RnMU16RXlOVEkyTXpVME5qTTRNekEwTURBek1qTTNORGcxTkRReE5ERTNNamcwTURjMk1qYzBNRE0wTmpreE16UTVOakV5TXpBek9ESTJPREF5T1RFek9USTFNemsyT1RnMk5EZ3hNVFV3T0RZd09URXlNREk1TlRRNU1UVTBNek13TnpBNE9UazFPVE0xTWprME16ZzVOakE1TURFNU16QTJNVGN4TURFMk56TXpOalV3TWpJM05EWTFNVFV5TnpReE9EUTBNelV5TWpnM01UTTFNRFU0TWprMk5UWTVOREV3TWpJek9ETTVPVGN5TkRZeE5UTXlPVEk1TlRVeE9EZ3lJbDBzSUZzaWJXRnpkR1Z5WDNObFkzSmxkQ0lzSUNJeE5EYzBNVEE1TVRBeU5qTXlNakU0T1RFME5qQXhOREU1T1RZeU56UXlNemcwTkRrM09ERXhNVE01TURFeU56QXpNRFU1TXpFeU56TXdNakExTnpFeU9EWTFOelkzTkRBM09EWTBOakUzTURNNU5ETXdNalF3TXpBMk16VTBPVEV5TWpZNU5EZ3hPRGt3TURjMk1qWTVNemd4TWpRek56TTRNekF5T0RJek5UUXpPVFE0TlRVeE9ESXpOemN4T0RZME9ETXdOVEUwTnpjME1qTTVNelF4TURJM01EWXhNamM0TnpVNU1UVTVNekU0TnpRNU5EazRNRFV4TURZeE16TTROVE13TlRFd05UZzNNVEU0Tmprd05qWTJPRFEzTnpnME56ZzROVEEyTmpRMU5qSXhORFkyTmpFMU9Ea3hNak0xTlRBeU16RTBNRFV6TWpZeE5UWXpNVFEzTWprM01USTVNRFUwTkRrd056Y3dNREl6TWpjMk5USTJOelEzTVRnMU1qTXpNRGd5TkRreE56Y3pNREE0TURreE5qUXlPRE01TmpBME5EazFNVEEzTnpJNE9ERTROelF5T0RVNU1qRTVPRFV4TVRjME5EYzJNVFF5T1RRME9UZzROamM1TnpJeE16UTROekl5TlRFME5qRTBNRFk1T1RrNE1qUXhNRFExTWpZME9EUXdPVGM1T0RNek1qZzNPREV3T0RNM01qTTVNRFl4TnprMk9EWXhOREEzTnpJek5UTXhOREl4TXpBMU56a3dOREk1TkRVNU9USTJOalUyT1RNeU1EazVNek0xTWpJd056azNOVFUwTmpZMk9UQXlNREEzT0RNME5Ea3hOakl4TnpjNU5ETXlNelEyTmpZME5UQTVPRFF6TXpBeU9ETXpNalkxTlRBM01ESTBPRGszT0RVNE1ESXdPVFU0TmprMU9EUXpPVFU0TnpZek16Y3pOak14TURFd01qVTJORFUwTnpBMk9ETTFOVGM0TmpnME5UazJNVFl5TmpreE5ERXhNVGt5TURJME9EYzVNREV4TmpFMU56QXlOekk0TnpVMk1qQTBOalkwTXpFd016UTFNVGt4TURVMk5ETTJORE14T1RnNE9Ua3dOemt5T0RnMU1UTXpPVGcwTXpBMk5UZzVOVFU0TmpVME16RXlPREV4TWpjeE1EQTNNRFl6TWpreU1qWTNOakUxT1RNeU5UYzBNamszTlRjaVhTd2dXeUpwWW1GdUlpd2dJakV6TkRZek5UUTFORGMxT0RZMk1qazBNVEE0TnpNNU16azNOekEwTURNMU16TTFOVGN5TnpNMU5qazVNRFE1TVRReU1qUTVPRE15TVRrek16a3lNRGN4TkRVM05qVTVNak13TlRVME5qQXpOVE15TnpRME9Ua3pOakU0TkRjNE56QTFNemN4TXpNMk9ESTNOekV6TnpBeU1EY3pOalkwT0RBeU9UWXpPVE0xTXpJNU16a3pOak0xTURrd056QTVNREk1TVRFM05qYzVOekU1TVRBNU9EQTROVGcwTnpZMk5UYzNNemN3TmpFM01ERTJPRFl3TURBek5qQTFOakEyTURBeU9UQTBOVE0zTlRrMk9EQXlORGszTlRnek5qQTVOalkzTkRFMU1UWTNOelV5TWpVME5qZ3dOekk0T1RJeU9UTXdNVGs1TlRRek16azBOVGt5TVRneU5ESXhPVEEyT0RJeU16RXlORFF5TkRJeE9UQTNNalEzTXpBMU9EY3hNams0TWpBeU1EWXdPRFF4TWpRek1qQXdNVFUwTWpRNU9ESXhPVEU1TnpFMk9EZzROalkyTVRJNU56SXpNemszTWprNU9UZ3lPVFUwTVRRMk5USTRNVFEwTlRrMU1UVTNPVEl6T1RJNE1qZzNNalUwTkRRek1qTTJNRGc1TlRBeE5UazJPVFV3TnpVMk9UUTBPVEUwTnpVNE1Ea3dNamt5TWpVek5UWTBPREF6TVRBNE9EZ3dNRFkwTURZNU5Ea3hPVFUwTlRZM016QXpNVEl5TlRVME9UQXhNRFk0TURBNE5ERXlORFl5TmpVME56WTBOall5TURnd056QTVORFl6TmpNek1UYzBPVGs0TlRJNE1Ea3lORGM1TmpJMk5UYzBORGd5T1RnNU16SXhNekF6TWpneE9UTTRNVEF3TmpZeU1USXlNamN4T1RreU16Z3pNemd5TkRrME5UWXdPVEk1T1RnNU5EUTJNRFU0TkRBNU1Ua3dOVGs0TVRrd01UazRORGs1T0RBMk5ERTFPRGs0TURnd01EQXhOVFF3TXpNNE16ZzFNalF3TURReE56a3pNakk0TlRNM01EY3lNemczTkRjME5EZzJPRGM1TnpjMk9EZ3hOVEk1T0RNM01qZzVOVGt3TmpFME5UZ3pORGN6T1RFNE16TTBNRGMzTVRNNU5UYzFOek15TmpBd05qZ3lNVE14TVRnM05EZzBOaUpkWFgwc0lDSnViMjVqWlNJNklDSTNPVFF5T0RBMU16RTFNRFl6TlRVM01qZzNPREV6TURjaWZRPT0ifSwibWltZS10eXBlIjoiYXBwbGljYXRpb24vanNvbiJ9XSwiY3JlZGVudGlhbF9wcmV2aWV3Ijp7IkB0eXBlIjoiZGlkOnNvdjpCekNic05ZaE1yakhpcVpEVFVBU0hnO3NwZWMvaXNzdWUtY3JlZGVudGlhbC8xLjAvY3JlZGVudGlhbC1wcmV2aWV3IiwiYXR0cmlidXRlcyI6W3sibmFtZSI6ImliYW4iLCJ2YWx1ZSI6IjEyMjMifSx7Im5hbWUiOiJiaWMiLCJ2YWx1ZSI6IjQ1NDU0In1dfX19LCJtaW1lLXR5cGUiOiJhcHBsaWNhdGlvbi9qc29uIn1dLCJzZXJ2aWNlcyI6WyJkaWQ6c292OlV2NTN2WjFTblMzTlBZTU1TcjRCYVEiXX0=";
}
