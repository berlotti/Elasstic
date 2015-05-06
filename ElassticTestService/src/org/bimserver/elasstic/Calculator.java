package org.bimserver.elasstic;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class Calculator {
	
	protected void writeRow(Sheet sheet, int rowNr, Object... values) {
	    Row row = sheet.createRow((short)rowNr);
	    int col = 0;
	    for (Object value : values) {
	    	Cell cell = row.createCell(col++);
	    	if (value instanceof String) {
	    		cell.setCellValue((String)value);
	    	} else if (value instanceof Double) {
	    		cell.setCellValue((Double)value);
	    	} else if (value instanceof Float) {
	    		cell.setCellValue((Float)value);
	    	} else if (value instanceof Integer) {
	    		cell.setCellValue((Integer)value);
	    	} else {
	    		throw new RuntimeException("Unimplemented: " + value);
	    	}
	    }
	}
}
