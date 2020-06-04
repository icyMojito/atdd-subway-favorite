package wooteco.subway.doc;

import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.restassured3.RestDocumentationFilter;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

public class Documentation {
	public static RestDocumentationFilter create(String identifier) {
		return document("favorite/" + identifier,
		                requestFields(
			                fieldWithPath("source").type(JsonFieldType.STRING).description("you")
		                ));
		// 알 수 없다..
	}
}
