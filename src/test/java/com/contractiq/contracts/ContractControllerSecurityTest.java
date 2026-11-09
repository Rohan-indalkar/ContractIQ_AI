package com.contractiq.contracts;

import com.contractiq.identity.JwtAuthenticationFilter;
import com.contractiq.identity.JwtTokenService;
import com.contractiq.identity.Role;
import com.contractiq.identity.SecurityConfiguration;
import com.contractiq.identity.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ContractController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class})
class ContractControllerSecurityTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ContractService contracts;
    @MockitoBean private JwtTokenService tokens;

    @Test
    void protectedContractListReturnsStructuredUnauthorizedResponseWithoutToken() throws Exception {
        mvc.perform(get("/api/v1/contracts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        verifyNoInteractions(contracts);
    }

    @Test
    void validTokenScopesContractListToItsOrganization() throws Exception {
        UserPrincipal principal = new UserPrincipal("user-a", "org-a", "a@example.test", Role.MEMBER);
        when(tokens.verify("signed-token")).thenReturn(principal);
        when(contracts.list(eq(principal), any(), any())).thenReturn(Page.empty());

        mvc.perform(get("/api/v1/contracts").header("Authorization", "Bearer signed-token"))
                .andExpect(status().isOk());

        verify(contracts).list(eq(principal), any(), any());
    }
}
