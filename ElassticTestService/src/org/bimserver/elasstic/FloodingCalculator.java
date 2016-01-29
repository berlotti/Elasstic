package org.bimserver.elasstic;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.bimserver.utils.PathUtils;

public class FloodingCalculator extends Calculator {
    public FloodingCalculator(String[] args) {
		super(args);
		
	}

	public static void main(String[] args) {
		new FloodingCalculator(args).start();
	}

	private void start() {
		try {
			Path dir = Paths.get("E:\\elasticlastfiles");
			
			Workbook wb = new HSSFWorkbook();
			Sheet sheet = wb.createSheet("Flooding");

			int row = 0;
			writeRow(sheet, row++, "GUID", "Name", "File", "Elevation (m)", "Department", "Classification", "Function", "Area (m2)");
			row++;
			
			for (Path file : PathUtils.list(dir)) {
				if (!file.getFileName().toString().endsWith(".ifc")) {
					continue;
				}
				List<Space> spaces = calculateSpaces(file);	
				System.out.println(file.getFileName().toString() + ": " + spaces.size());
				
				for (Space space : spaces) {
					writeRow(sheet, row++, space.getGuid(), space.getName(), file.getFileName().toString(), space.getLowestLevel() / 1000, space.getDepartment(), space.getClassification(), space.getFunction(), space.getArea());
				}
			}
			
			Path file2 = dir.resolve("flooding.xls");
			FileOutputStream fileOut = new FileOutputStream(file2.toFile());
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
