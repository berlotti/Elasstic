package org.bimserver.elasstic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.bimserver.models.ifc2x3tc1.IfcAreaMeasure;
import org.bimserver.models.ifc2x3tc1.IfcIdentifier;
import org.bimserver.models.ifc2x3tc1.IfcLabel;
import org.bimserver.models.ifc2x3tc1.IfcLengthMeasure;
import org.bimserver.models.ifc2x3tc1.IfcProperty;
import org.bimserver.models.ifc2x3tc1.IfcPropertySet;
import org.bimserver.models.ifc2x3tc1.IfcPropertySetDefinition;
import org.bimserver.models.ifc2x3tc1.IfcPropertySingleValue;
import org.bimserver.models.ifc2x3tc1.IfcRelDefines;
import org.bimserver.models.ifc2x3tc1.IfcRelDefinesByProperties;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.models.ifc2x3tc1.IfcText;
import org.bimserver.models.ifc2x3tc1.IfcValue;

public class AreasCalculator extends Calculator {
	public AreasCalculator(String[] args) {
		super(args);
	}

	public static void main(String[] args) {
		new AreasCalculator(args).start();
	}
	
	private class Areas {
		public String department;
		public String classification;
		public int nrAreas;
		public double totalArea;

		public Areas(String department, String classification) {
			this.department = department;
			this.classification = classification;
		}
	}

	private void start() {
		try {
			Workbook wb = new HSSFWorkbook();

			Map<String, Areas> totalmap = new TreeMap<>();
			
			File dir = new File("E:\\elasticlastfiles");
			for (File file : dir.listFiles()) {
				if (!file.getName().endsWith(".ifc")) {
					continue;
				}
				Map<String, Areas> map = new TreeMap<>();
				Map<String, Double> totalAreaMap = new HashMap<>();

				List<Space> spaces = calculateSpaces(file);
				for (Space space : spaces) {
					Areas areas = map.get(space.getDepartment() + "_" + space.getClassification());
					if (areas == null) {
						areas = new Areas(space.getDepartment(), space.getClassification());
						map.put(space.getDepartment() + "_" + space.getClassification(), areas);
					}
					areas.totalArea += space.getArea();
					areas.nrAreas++;
					
					Areas totalAreas = totalmap.get(space.getDepartment() + "_" + space.getClassification());
					if (totalAreas == null) {
						totalAreas = new Areas(space.getDepartment(), space.getClassification());
						totalmap.put(space.getDepartment() + "_" + space.getClassification(), totalAreas);
					}
					totalAreas.totalArea += space.getArea();
					totalAreas.nrAreas++;

					Double totalArea = totalAreaMap.get(space.getDepartment());
					if (totalArea == null) {
						totalAreaMap.put(space.getDepartment(), space.getArea());
					} else {
						totalAreaMap.put(space.getDepartment(), totalAreaMap.get(space.getDepartment()) + space.getArea());
					}
				}
				
				Sheet sheet = wb.createSheet(file.getName());
				int row = 0;
				
				writeRow(sheet, row++, "Department", "Classification", "# Areas", "Total m2");
				row++;
				for (Areas areas : map.values()) {
					writeRow(sheet, row++, areas.department == null ? "No Department" : areas.department, areas.classification, areas.nrAreas, areas.totalArea);
				}

			}
			
			Map<String, Splitted> splittedmap = new HashMap<String, AreasCalculator.Splitted>();
			
			for (Areas areas : totalmap.values()) {
				Splitted splitted = splittedmap.get(areas.department);
				if (splitted == null) {
					splitted = new Splitted(areas.department, 0, 0, 0);
					splittedmap.put(areas.department, splitted);
				}
				splitted.totalm2 += areas.totalArea;
				splitted.nrobjects += areas.nrAreas;
				if (getMappings().get("Functional").contains(areas.classification)) {
					splitted.functionalm2 += areas.totalArea;
				} else if (getMappings().get("Circulation").contains(areas.classification)) {
					splitted.circulationm2 += areas.totalArea;
				} else {
					splitted.otherm2 += areas.totalArea;
				}
			}
			
			Sheet totalSheet = wb.createSheet("Total");
			writeRow(totalSheet, 0, "Department", "# Areas", "Functional m2", "Circulation m2", "Total m2");
			int row = 2;
			for (Splitted splitted : splittedmap.values()) {
				writeRow(totalSheet, row++, splitted.department == null ? "No Department" : splitted.department, splitted.nrobjects, splitted.functionalm2, splitted.circulationm2, splitted.totalm2);
			}
			File file2 = new File(dir, "elasstic.xls");
			FileOutputStream fileOut = new FileOutputStream(file2);
			wb.write(fileOut);
			wb.close();
			fileOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	class Splitted {
		public int nrobjects;
		public double otherm2;
		private String department;
		private double totalm2;
		private double functionalm2;
		private double circulationm2;

		public Splitted(String department, double totalm2, double functionalm2, double circulationm2) {
			this.department = department;
			this.totalm2 = totalm2;
			this.functionalm2 = functionalm2;
			this.circulationm2 = circulationm2;
			
		}
	}
	
	public void dumpProperties(IfcSpace ifcObject) {
		for (IfcRelDefines ifcRelDefines : ifcObject.getIsDefinedBy()) {
			if (ifcRelDefines instanceof IfcRelDefinesByProperties) {
				IfcRelDefinesByProperties ifcRelDefinesByProperties = (IfcRelDefinesByProperties)ifcRelDefines;
				IfcPropertySetDefinition relatingPropertyDefinition = ifcRelDefinesByProperties.getRelatingPropertyDefinition();
				if (relatingPropertyDefinition instanceof IfcPropertySet) {
					IfcPropertySet ifcPropertySet = (IfcPropertySet)relatingPropertyDefinition;
					for (IfcProperty ifcProperty : ifcPropertySet.getHasProperties()) {
						if (ifcProperty instanceof IfcPropertySingleValue) {
							IfcPropertySingleValue ifcPropertySingleValue = (IfcPropertySingleValue)ifcProperty;
							IfcValue value = ifcPropertySingleValue.getNominalValue();
							if (value instanceof IfcText) {
								IfcText ifcText = (IfcText)value;
								System.out.println(ifcPropertySingleValue.getName() + ": " + ifcText.getWrappedValue());
							} else if (value instanceof IfcIdentifier) {
								IfcIdentifier ifcIdentifier = (IfcIdentifier)value;
								System.out.println(ifcPropertySingleValue.getName() + ": " + ifcIdentifier.getWrappedValue());
							} else if (value instanceof IfcLabel) {
								IfcLabel ifcIdentifier = (IfcLabel)value;
								System.out.println(ifcPropertySingleValue.getName() + ": " + ifcIdentifier.getWrappedValue());
							} else if (value instanceof IfcLengthMeasure) {
								IfcLengthMeasure ifcIdentifier = (IfcLengthMeasure)value;
								System.out.println(ifcPropertySingleValue.getName() + ": " + ifcIdentifier.getWrappedValue());
							} else if (value instanceof IfcAreaMeasure) {
								IfcAreaMeasure ifcIdentifier = (IfcAreaMeasure)value;
								System.out.println(ifcPropertySingleValue.getName() + ": " + ifcIdentifier.getWrappedValue());
							} else {
								System.out.println("Unimplemented: " + value);
							}
						}
					}
				}
			}
		}
	}	
}