package com.hccake.ballcat.admin.modules.log.controller;

import com.hccake.ballcat.admin.modules.log.model.qo.AdminOperationLogQO;
import com.hccake.ballcat.admin.modules.log.model.vo.AdminOperationLogVO;
import com.hccake.ballcat.admin.modules.log.service.OperationLogAdminService;
import com.hccake.ballcat.common.core.domain.PageParam;
import com.hccake.ballcat.common.core.domain.PageResult;
import com.hccake.ballcat.common.core.result.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志
 *
 * @author hccake
 * @date 2019-10-15 20:42:32
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/log/adminoperationlog")
@Api(value = "adminoperationlog", tags = "操作日志管理")
public class AdminOperationLogController {

	private final OperationLogAdminService operationLogAdminService;

	/**
	 * 分页查询
	 * @param pageParam 分页参数
	 * @param adminOperationLogQO 操作日志
	 * @return R
	 */
	@ApiOperation(value = "分页查询", notes = "分页查询")
	@GetMapping("/page")
	@PreAuthorize("@per.hasPermission('log:adminoperationlog:read')")
	public R<PageResult<AdminOperationLogVO>> getOperationLogAdminPage(PageParam pageParam,
			AdminOperationLogQO adminOperationLogQO) {
		return R.ok(operationLogAdminService.queryPage(pageParam, adminOperationLogQO));
	}

}
