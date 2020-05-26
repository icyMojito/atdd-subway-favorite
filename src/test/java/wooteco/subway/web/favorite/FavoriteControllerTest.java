package wooteco.subway.web.favorite;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import wooteco.subway.acceptance.favorite.dto.FavoritePathResponse;
import wooteco.subway.acceptance.favorite.dto.StationPathResponse;
import wooteco.subway.domain.member.Member;
import wooteco.subway.domain.path.FavoritePath;
import wooteco.subway.domain.station.Station;
import wooteco.subway.infra.JwtTokenProvider;
import wooteco.subway.service.FavoriteService;
import wooteco.subway.service.member.MemberService;
import wooteco.subway.service.station.dto.StationResponse;
import wooteco.subway.web.FavoriteController;
import wooteco.subway.web.dto.ExceptionResponse;
import wooteco.subway.web.dto.FavoritePathRequest;
import wooteco.subway.web.member.AuthorizationExtractor;
import wooteco.subway.web.member.interceptor.BearerAuthInterceptor;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static wooteco.subway.AcceptanceTest.*;

@WebMvcTest(controllers = FavoriteController.class)
@AutoConfigureMockMvc
@Import({BearerAuthInterceptor.class, AuthorizationExtractor.class, JwtTokenProvider.class})
class FavoriteControllerTest {
	private static final String INVALID_TOKEN = "";

	@MockBean
	private FavoriteService favoriteService;

	@MockBean
	private MemberService memberService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private WebApplicationContext ctx;

	@Value("${security.jwt.token.secret-key}")
	private String secretKey;

	@Value("${security.jwt.token.expire-length}")
	private long validityInMilliseconds;

	@BeforeEach
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
				.addFilters(new CharacterEncodingFilter("UTF-8", true))  // 필터 추가
				.alwaysDo(print())
				.build();
	}

	@DisplayName("토큰이 유효하지 않는 경우 즐겨찾기 등록에 실패하는지 확인")
	@Test
	void authFailedWhenRegisterFavoritePath() throws Exception {
		FavoritePathRequest request = new FavoritePathRequest(STATION_NAME_KANGNAM, STATION_NAME_HANTI);
		String stringContent = objectMapper.writeValueAsString(request);

		MvcResult result = register(INVALID_TOKEN, stringContent, status().isUnauthorized());

		String stringBody = result.getResponse().getContentAsString();
		ExceptionResponse response = objectMapper.readValue(stringBody, ExceptionResponse.class);
		assertThat(response.getErrorMessage()).isEqualTo("유효하지 않은 토큰입니다!");
	}

	@DisplayName("토큰이 유효하지 않는 경우 즐겨찾기 조회에 실패하는지 확인")
	@Test
	void authFailedWhenRetrieveFavoritePath() throws Exception {
		MvcResult result = retrieve(INVALID_TOKEN, status().isUnauthorized());
		String stringBody = result.getResponse().getContentAsString();
		ExceptionResponse response = objectMapper.readValue(stringBody, ExceptionResponse.class);
		assertThat(response.getErrorMessage()).isEqualTo("유효하지 않은 토큰입니다!");
	}

	@DisplayName("토큰이 유효하지 않는 경우 즐겨찾기 삭제에 실패하는지 확인")
	@Test
	void authFailedWhenDeleteFavoritePath() throws Exception {
		MvcResult result = delete(INVALID_TOKEN, status().isUnauthorized());

		String stringBody = result.getResponse().getContentAsString();
		ExceptionResponse response = objectMapper.readValue(stringBody, ExceptionResponse.class);
		assertThat(response.getErrorMessage()).isEqualTo("유효하지 않은 토큰입니다!");
	}

	@DisplayName("토큰이 유효하면 즐겨찾기 등록에 성공하는지 확인")
	@Test
	void registerFavoritePath() throws Exception {
		FavoritePathRequest request = new FavoritePathRequest(STATION_NAME_KANGNAM, STATION_NAME_HANTI);
		String stringContent = objectMapper.writeValueAsString(request);
		Member member = new Member(1L, TEST_USER_EMAIL, TEST_USER_NAME, TEST_USER_PASSWORD);

		BDDMockito.when(favoriteService.registerPath(member, STATION_NAME_KANGNAM, STATION_NAME_HANTI))
				.thenReturn(new FavoritePath(1L, 1L, 2L));

		String token =
				"bearer " + new JwtTokenProvider(secretKey, validityInMilliseconds).createToken(TEST_USER_EMAIL);
		MvcResult result = register(token, stringContent, status().isCreated());

		assertThat(result.getResponse().getHeader("Location")).isNotNull();
	}

	private MvcResult register(String token, String content, ResultMatcher status) throws Exception {
		return mockMvc.perform(
				post("/favorite/me")
						.header("Authorization", token)
						.content(content)
						.contentType(MediaType.APPLICATION_JSON_VALUE))
				.andDo(print())
				.andExpect(status)
				.andReturn();
	}

	@DisplayName("토큰이 유효하면 즐겨찾기 조회에 성공하는지 확인")
	@Test
	void retrieveFavoritePath() throws Exception {
		Member member = new Member(1L, TEST_USER_EMAIL, TEST_USER_NAME, TEST_USER_PASSWORD);
		Station kangnam = new Station(1L, STATION_NAME_KANGNAM);
		Station hanti = new Station(2L, STATION_NAME_HANTI);
		Station dogok = new Station(3L, STATION_NAME_DOGOK);
		Station yangjae = new Station(4L, STATION_NAME_YANGJAE);

		BDDMockito.when(favoriteService.retrievePath(member))
				.thenReturn(Arrays.asList(new StationPathResponse(1L, StationResponse.of(kangnam),
				                                                  StationResponse.of(hanti)),
				                          new StationPathResponse(2L, StationResponse.of(dogok),
				                                                  StationResponse.of(yangjae))));
		BDDMockito.when(memberService.findMemberByEmail(TEST_USER_EMAIL)).thenReturn(member);

		String token =
				"bearer " + new JwtTokenProvider(secretKey, validityInMilliseconds).createToken(TEST_USER_EMAIL);
		MvcResult result = retrieve(token, status().isOk());

		String body = result.getResponse().getContentAsString();
		FavoritePathResponse response = objectMapper.readValue(body, FavoritePathResponse.class);

		assertThat(response.getFavoritePaths()).hasSize(2);
		assertThat(response.getFavoritePaths().get(0).getSource().getId()).isEqualTo(1L);
	}

	private MvcResult retrieve(String token, ResultMatcher status) throws Exception {
		return mockMvc.perform(
				MockMvcRequestBuilders.get("/favorite/me")
						.header("Authorization", token))
				.andDo(print())
				.andExpect(status)
				.andReturn();
	}

	@DisplayName("토큰이 유효하면 즐겨찾기 삭제에 성공하는지 확인")
	@Test
	void deleteFavoritePath() throws Exception {
		Member member = new Member(1L, TEST_USER_EMAIL, TEST_USER_NAME, TEST_USER_PASSWORD);
		Station dogok = new Station(3L, STATION_NAME_DOGOK);
		Station yangjae = new Station(4L, STATION_NAME_YANGJAE);

		BDDMockito.when(favoriteService.retrievePath(member))
				.thenReturn(Arrays.asList(new StationPathResponse(2L, StationResponse.of(dogok),
				                                                  StationResponse.of(yangjae))));
		BDDMockito.when(memberService.findMemberByEmail(TEST_USER_EMAIL)).thenReturn(member);

		String token =
				"bearer " + new JwtTokenProvider(secretKey, validityInMilliseconds).createToken(TEST_USER_EMAIL);
		delete(token, status().isNoContent());

		MvcResult result = retrieve(token, status().isOk());
		String body = result.getResponse().getContentAsString();
		FavoritePathResponse response = objectMapper.readValue(body, FavoritePathResponse.class);

		assertThat(response.getFavoritePaths()).hasSize(1);
		assertThat(response.getFavoritePaths().get(0).getSource().getId()).isEqualTo(3L);
	}

	private MvcResult delete(String token, ResultMatcher statusCode) throws Exception {
		return mockMvc.perform(
				MockMvcRequestBuilders.delete("/favorite/me/1")
						.header("Authorization", token))
				.andDo(print())
				.andExpect(statusCode)
				.andReturn();
	}
}