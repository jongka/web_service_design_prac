package kr.ac.jbnu.bjh.web_service_design_hw1;

import kr.ac.jbnu.bjh.web_service_design_hw1.api.v3.ItemController3;
import kr.ac.jbnu.bjh.web_service_design_hw1.config.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ItemController3Tests {
    private MockMvc mvc;
    private final String base = "/api/v3/items";
    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new ItemController3())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(new LoggingInterceptor()).build();
    }
    @Test
    void allEightApis() throws Exception {
        mvc.perform(post(base).contentType("application/json").content("{\"name\":\"pen\",\"price\":100}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", base + "/1"));
        mvc.perform(post(base + "/bulk").contentType("application/json")
                .content("[{\"name\":\"book\",\"price\":200},{\"name\":\"bag\",\"price\":300}]"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get(base)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get(base + "/1")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("pen"));
        mvc.perform(put(base + "/1").contentType("application/json").content("{\"name\":\"pencil\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("pencil"));
        mvc.perform(put(base + "/bulk/prices").contentType("application/json")
                .content("[{\"id\":2,\"price\":400},{\"id\":3,\"price\":500}]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].price").value(400));
        mvc.perform(delete(base + "/1")).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(delete(base + "/bulk").param("ids", "2,3"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get(base)).andExpect(jsonPath("$.length()").value(0));
    }
    @Test
    void errorsHaveStandardFormat() throws Exception {
        mvc.perform(post(base).contentType("application/json").content("{\"name\":\"\",\"price\":-1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.statusCode").value(400));
        mvc.perform(post(base).contentType("application/json").content("{\"name\":"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.statusCode").value(400));
        mvc.perform(get(base + "/abc")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.statusCode").value(400));
        mvc.perform(get(base + "/99")).andExpect(status().isNotFound()).andExpect(jsonPath("$.statusCode").value(404));
        for (int code : new int[]{500, 503}) {
            mvc.perform(get(base).header("X-Demo-Error", String.valueOf(code)))
                    .andExpect(status().is(code)).andExpect(jsonPath("$.statusCode").value(code))
                    .andExpect(jsonPath("$.message").value("서버 처리 중 오류가 발생했습니다."));
        }
    }
    @Test
    void failedBulkOperationsPreserveData() throws Exception {
        mvc.perform(post(base).contentType("application/json").content("{\"name\":\"pen\",\"price\":100}"));
        mvc.perform(put(base + "/bulk/prices").contentType("application/json")
                .content("[{\"id\":1,\"price\":999},{\"id\":99,\"price\":200}]"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(base + "/bulk").param("ids", "1,99")).andExpect(status().isNotFound());
        mvc.perform(get(base + "/1")).andExpect(status().isOk()).andExpect(jsonPath("$.price").value(100));
    }
}