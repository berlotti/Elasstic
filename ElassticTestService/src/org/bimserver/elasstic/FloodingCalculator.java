package org.bimserver.elasstic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class FloodingCalculator extends Calculator {
    public FloodingCalculator(String[] args) {
		super(args);
		
	}

	public static void main(String[] args) {
		new FloodingCalculator(args).start();
	}

	private void start() {
		try {
			File dir = new File("E:\\elasticlastfiles");
			
			Workbook wb = new HSSFWorkbook();
			Sheet sheet = wb.createSheet("Flooding");

			int row = 0;
			writeRow(sheet, row++, "GUID", "Name", "File", "Elevation (m)", "Department", "Classification", "Function", "Area (m2)");
			row++;
			
			for (File file : dir.listFiles()) {
				if (!file.getName().endsWith(".ifc")) {
					continue;
				}
				List<Space> spaces = calculateSpaces(file);	
				System.out.println(file.getName() + ": " + spaces.size());
				
				for (Space space : spaces) {
					writeRow(sheet, row++, space.getGuid(), space.getName(), file.getName(), space.getLowestLevel() / 1000, space.getDepartment(), space.getClassification(), space.getFunction(), space.getArea());
				}
			}
			
			File file2 = new File(dir, "flooding.xls");
			FileOutputStream fileOut = new FileOutputStream(file2);
			wb.write(fileOut);
			wb.close();
			fileOut.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
