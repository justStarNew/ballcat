package com.hccake.ballcat.admin.modules.log.handler;

import cn.hutool.core.util.URLUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hccake.ballcat.admin.modules.log.model.entity.AdminAccessLog;
import com.hccake.ballcat.admin.modules.log.thread.AccessLogAdminSaveThread;
import com.hccake.ballcat.admin.oauth.SysUserDetails;
import com.hccake.ballcat.admin.oauth.util.SecurityUtils;
import com.hccake.ballcat.commom.log.access.handler.AccessLogHandler;
import com.hccake.ballcat.commom.log.constant.LogConstant;
import com.hccake.ballcat.commom.log.util.LogUtils;
import com.hccake.ballcat.common.core.desensite.enums.RegexDesensitizationTypeEnum;
import com.hccake.ballcat.common.core.desensite.handler.RegexDesensitizationHandler;
import com.hccake.ballcat.common.core.util.IPUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 访问日志
 *
 * @author hccake
 * @date 2019-10-16 16:09:25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAccessLogHandler implements AccessLogHandler<AdminAccessLog> {

	private final static String APPLICATION_JSON = "application/json";

	private final AccessLogAdminSaveThread accessLogAdminSaveThread;

	private final ObjectMapper objectMapper;

	/**
	 * 需要脱敏记录的参数
	 */
	private final List<String> needDesensitizeParams = Arrays.asList("password", "pass", "passConfirm");

	/**
	 * 脱敏处理器
	 */
	private final RegexDesensitizationHandler regexDesensitizationHandler = new RegexDesensitizationHandler();

	/**
	 * 生产一个日志
	 * @return accessLog
	 * @param request 请求信息
	 * @param response 响应信息
	 * @param time 执行时长
	 * @param myThrowable 异常信息
	 */
	@Override
	public AdminAccessLog prodLog(HttpServletRequest request, HttpServletResponse response, Long time,
			Throwable myThrowable) {
		Object matchingPatternAttr = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		String matchingPattern = matchingPatternAttr == null ? "" : String.valueOf(matchingPatternAttr);
		// @formatter:off
		String uri = URLUtil.getPath(request.getRequestURI());
		AdminAccessLog adminAccessLog = new AdminAccessLog()
				.setTraceId(MDC.get(LogConstant.TRACE_ID))
				.setCreateTime(LocalDateTime.now())
				.setTime(time)
				.setIp(IPUtil.getIpAddr(request))
				.setMethod(request.getMethod())
				.setUserAgent(request.getHeader("user-agent"))
				.setUri(uri)
				.setMatchingPattern(matchingPattern)
				.setErrorMsg(Optional.ofNullable(myThrowable).map(Throwable::getMessage).orElse(""))
				.setHttpStatus(response.getStatus());
		// @formatter:on

		// 参数获取
		String params = getParams(request);
		adminAccessLog.setReqParams(params);

		// 非文件上传请求，记录body，用户改密时不记录body
		// TODO 使用注解控制此次请求是否记录body，更方便个性化定制
		if (!LogUtils.isMultipartContent(request) && "/sysuser/pass/{userId}".equals(uri)) {
			adminAccessLog.setReqBody(LogUtils.getRequestBody(request));
		}

		// 只记录响应头为 application/json 的返回数据
		// 后台日志对于分页数据请求，不记录返回值
		if (!uri.endsWith("/page") && response.getContentType() != null
				&& response.getContentType().contains(APPLICATION_JSON)) {
			adminAccessLog.setResult(LogUtils.getResponseBody(request, response));
		}

		// 如果登陆用户 则记录用户名和用户id
		Optional.ofNullable(SecurityUtils.getSysUserDetails()).map(SysUserDetails::getSysUser).ifPresent(x -> {
			adminAccessLog.setUserId(x.getUserId());
			adminAccessLog.setUsername(x.getUsername());
		});

		return adminAccessLog;
	}

	/**
	 * 获取参数信息
	 * @param request 请求信息
	 * @return 请求参数
	 */
	public String getParams(HttpServletRequest request) {
		String params;
		try {
			Map<String, String[]> parameterMap = request.getParameterMap();
			for (String paramKey : needDesensitizeParams) {
				String[] values = parameterMap.get(paramKey);
				if (values != null && values.length != 0) {
					String value = regexDesensitizationHandler.handle(values[0],
							RegexDesensitizationTypeEnum.ENCRYPTED_PASSWORD);
					parameterMap.put(paramKey, new String[] { value });
				}
			}
			params = objectMapper.writeValueAsString(parameterMap);
		}
		catch (JsonProcessingException e) {
			params = "记录参数异常";
			log.error("[prodLog]，参数获取序列化异常", e);
		}
		return params;
	}

	/**
	 * 记录日志
	 * @param accessLog 访问日志
	 */
	@Override
	public void saveLog(AdminAccessLog accessLog) {
		accessLogAdminSaveThread.putObject(accessLog);
	}

}
