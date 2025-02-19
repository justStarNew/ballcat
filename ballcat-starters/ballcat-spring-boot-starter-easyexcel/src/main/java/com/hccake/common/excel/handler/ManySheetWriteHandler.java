package com.hccake.common.excel.handler;

import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.hccake.common.excel.annotation.ResponseExcel;
import com.hccake.common.excel.config.ExcelConfigProperties;
import com.hccake.common.excel.enhance.WriterBuilderEnhancer;
import com.hccake.common.excel.kit.ExcelException;
import lombok.SneakyThrows;
import org.springframework.beans.factory.ObjectProvider;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author lengleng
 * @date 2020/3/29
 */
public class ManySheetWriteHandler extends AbstractSheetWriteHandler {

	public ManySheetWriteHandler(ExcelConfigProperties configProperties,
			ObjectProvider<List<Converter<?>>> converterProvider, WriterBuilderEnhancer excelWriterBuilderEnhance) {
		super(configProperties, converterProvider, excelWriterBuilderEnhance);
	}

	/**
	 * 当且仅当List不为空且List中的元素也是List 才返回true
	 * @param obj 返回对象
	 * @return
	 */
	@Override
	public boolean support(Object obj) {
		if (obj instanceof List) {
			List objList = (List) obj;
			return !objList.isEmpty() && objList.get(0) instanceof List;
		}
		else {
			throw new ExcelException("@ResponseExcel 返回值必须为List类型");
		}
	}

	@Override
	@SneakyThrows
	public void write(Object obj, HttpServletResponse response, ResponseExcel responseExcel) {
		List objList = (List) obj;
		ExcelWriter excelWriter = getExcelWriter(response, responseExcel);

		String[] sheets = responseExcel.sheet();
		WriteSheet sheet;
		for (int i = 0; i < sheets.length; i++) {
			List eleList = (List) objList.get(i);
			Class<?> dataClass = eleList.get(0).getClass();

			// 创建sheet
			sheet = this.sheet(i, responseExcel.sheet()[i], dataClass, responseExcel.template(),
					responseExcel.headGenerator());
			// 写入sheet
			excelWriter.write((List) objList.get(i), sheet);
		}
		excelWriter.finish();
	}

}
