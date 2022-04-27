/*
 * Copyright 2022-Present Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.authfoundation.client

import com.google.common.truth.Truth.assertThat
import com.okta.authfoundation.credential.Token
import com.okta.authfoundation.jwt.Jwt
import com.okta.testhelpers.OktaRule
import com.okta.testhelpers.RequestMatchers.body
import com.okta.testhelpers.RequestMatchers.header
import com.okta.testhelpers.RequestMatchers.method
import com.okta.testhelpers.RequestMatchers.path
import com.okta.testhelpers.testBodyFromFile
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class OidcClientIdTokenValidationFailureTest {
    private val mockPrefix = "client_test_responses"

    @get:Rule val oktaRule = OktaRule(
        idTokenValidator = object : IdTokenValidator {
            override var issuedAtGracePeriodInSeconds: Int = 600
            override suspend fun validate(oidcClient: OidcClient, idToken: Jwt, nonce: String?, maxAge: Int?) {
                throw IllegalStateException("Failure!")
            }
        }
    )

    @Test fun testRefreshTokenIdTokenValidationFailure(): Unit = runBlocking {
        oktaRule.enqueue(
            method("POST"),
            header("content-type", "application/x-www-form-urlencoded"),
            path("/oauth2/default/v1/token"),
            body("client_id=unit_test_client_id&grant_type=refresh_token&refresh_token=ExampleRefreshToken&scope=openid%20email%20profile%20offline_access"),
        ) { response ->
            response.testBodyFromFile("$mockPrefix/token.json")
        }
        val result = oktaRule.createOidcClient().refreshToken("ExampleRefreshToken")
        val exception = (result as OidcClientResult.Error<Token>).exception
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(exception).hasMessageThat().isEqualTo("Failure!")
    }
}
