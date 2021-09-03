package org.hyperledger.bpa.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InvitationParserTest {

    @Test
    void TestParseReceiveInvitation() {
        InvitationParser p = new InvitationParser();
        p.setMapper(new ObjectMapper());

        InvitationParser.Invitation invitation = p.parseInvitation(this.invitation);
        Assertions.assertFalse(invitation.isOob());
        Assertions.assertTrue(invitation.isParsed());
        Assertions.assertNotNull(invitation.getInvitation());
    }

    @Test
    void TestParseReceiveOOBInvitation() {
        InvitationParser p = new InvitationParser();
        p.setMapper(new ObjectMapper());

        InvitationParser.Invitation invitation = p.parseInvitation(oob);
        Assertions.assertTrue(invitation.isOob());
        Assertions.assertTrue(invitation.isParsed());
        Assertions.assertNotNull(invitation.getInvitation());
    }

    private final String invitation = "ewogICAgIkB0eXBlIjogImRpZDpzb3Y6QnpDYnNOWWhNcmpIaXFaRFRVQVNIZztzcGVjL2Nvbm5lY3Rpb25zLzEuMC9pbnZpdGF0aW9uIiwKICAgICJAaWQiOiAiNGQ1OGJhZjktZDIwOS00MTE4LThkOTQtNGE0OTBlNGEwNGFhIiwKICAgICJzZXJ2aWNlRW5kcG9pbnQiOiAiaHR0cDovL2hvc3QuZG9ja2VyLmludGVybmFsOjgwMzAiLAogICAgInJlY2lwaWVudEtleXMiOiBbCiAgICAgICAgIjZCTlF1dFJIalNWNmJwQ0E2djVkRVB2NW12dWlRS2hyc256cEN4dUgzdXdqIgogICAgXSwKICAgICJsYWJlbCI6ICJCdXNpbmVzcyBQYXJ0bmVyIEFnZW50IDEiCn0=";
    private final String oob = "eyJAdHlwZSI6ICJkaWQ6c292OkJ6Q2JzTlloTXJqSGlxWkRUVUFTSGc7c3BlYy9vdXQtb2YtYmFuZC8xLjAvaW52aXRhdGlvbiIsICJAaWQiOiAiMmZhYmJhNzYtZTlhNy00Yzk4LTg2ZjMtMTFkNGE1MTYzYjQyIiwgImhhbmRzaGFrZV9wcm90b2NvbHMiOiBbImRpZDpzb3Y6QnpDYnNOWWhNcmpIaXFaRFRVQVNIZztzcGVjL2RpZGV4Y2hhbmdlLzEuMCJdLCAic2VydmljZXMiOiBbImRpZDpzb3Y6RXJhWUNESlVQc0NoYmt3N1MxdlY5NiJdLCAibGFiZWwiOiAiYm9iIn0=";
}
