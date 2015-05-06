package org.bimserver.elasstic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.bimserver.LocalDevSetup;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.emf.Schema;
import org.bimserver.models.ifc2x3tc1.IfcAreaMeasure;
import org.bimserver.models.ifc2x3tc1.IfcExtrudedAreaSolid;
import org.bimserver.models.ifc2x3tc1.IfcIdentifier;
import org.bimserver.models.ifc2x3tc1.IfcLabel;
import org.bimserver.models.ifc2x3tc1.IfcLengthMeasure;
import org.bimserver.models.ifc2x3tc1.IfcProductRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcProfileDef;
import org.bimserver.models.ifc2x3tc1.IfcProperty;
import org.bimserver.models.ifc2x3tc1.IfcPropertySet;
import org.bimserver.models.ifc2x3tc1.IfcPropertySetDefinition;
import org.bimserver.models.ifc2x3tc1.IfcPropertySingleValue;
import org.bimserver.models.ifc2x3tc1.IfcRectangleProfileDef;
import org.bimserver.models.ifc2x3tc1.IfcRelDefines;
import org.bimserver.models.ifc2x3tc1.IfcRelDefinesByProperties;
import org.bimserver.models.ifc2x3tc1.IfcRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcRepresentationItem;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.models.ifc2x3tc1.IfcText;
import org.bimserver.models.ifc2x3tc1.IfcValue;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginException;
import org.bimserver.plugins.PluginManager;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.Deserializer;
import org.bimserver.plugins.deserializers.DeserializerPlugin;
import org.bimserver.plugins.renderengine.IndexFormat;
import org.bimserver.plugins.renderengine.Precision;
import org.bimserver.plugins.renderengine.RenderEngine;
import org.bimserver.plugins.renderengine.RenderEngineInstance;
import org.bimserver.plugins.renderengine.RenderEngineModel;
import org.bimserver.plugins.renderengine.RenderEnginePlugin;
import org.bimserver.plugins.renderengine.RenderEngineSettings;

public class AreasCalculator extends Calculator {
	public static void main(String[] args) {
		new AreasCalculator().start(args);
	}
	
	private class Areas {
		public String department;
		public Areas(String department, String name) {
			this.department = department;
			this.classification = name;
		}
		public String classification;
		public int nrAreas;
		public double totalArea;
	}

	private void start(String[] args) {
	    NumberFormat formatter = new DecimalFormat("#0.00");     
		try {
			Map<String, List<String>> mappings = new HashMap<>();
			
			Workbook inputWb = WorkbookFactory.create(new File("input/Relation classification functional areas.xlsx"));
			Sheet firstSheet = inputWb.getSheetAt(0);
			String currentFunction = null;
			List<String> list = null;
			for (int i=firstSheet.getFirstRowNum(); i<=firstSheet.getLastRowNum(); i++) {
				Row row = firstSheet.getRow(i);
				if (row != null) {
					Cell cell0 = row.getCell(0);
					if (cell0 != null && !cell0.getStringCellValue().equals("")) {
						currentFunction = cell0.getStringCellValue();
						list = new ArrayList<>();
						mappings.put(currentFunction, list);
					}
					list.add(row.getCell(1).getStringCellValue());
				}
			}
			
			PluginManager pluginManager = LocalDevSetup.setupPluginManager(args);
			DeserializerPlugin ifcDeserializerPlugin = pluginManager.getFirstDeserializer("ifc", Schema.IFC2X3TC1, true);
			File dir = new File("E:\\elasticlastfiles");
			
			Workbook wb = new HSSFWorkbook();

			Map<String, Areas> totalmap = new TreeMap<>();
			
			for (File file : dir.listFiles()) {
				if (!file.getName().endsWith(".ifc")) {
					continue;
				}
				Deserializer ifcDeserializer  = ifcDeserializerPlugin.createDeserializer(new PluginConfiguration());
				ifcDeserializer.init(pluginManager.getMetaDataManager().getPackageMetaData("ifc2x3tc1"));
				IfcModelInterface model = ifcDeserializer.read(file);
				
				RenderEnginePlugin renderEnginePlugin = pluginManager.getRenderEngine("org.bimserver.ifcengine.JvmRenderEnginePlugin", true);
				RenderEngine renderEngine = renderEnginePlugin.createRenderEngine(new PluginConfiguration(), "ifc2x3tc1");
				renderEngine.init();
				
				final RenderEngineSettings settings = new RenderEngineSettings();
				settings.setPrecision(Precision.SINGLE);
				settings.setIndexFormat(IndexFormat.AUTO_DETECT);
				settings.setGenerateNormals(true);
				settings.setGenerateTriangles(true);
				settings.setGenerateWireFrame(false);
				
				RenderEngineModel renderEngineModel = renderEngine.openModel(file);
				renderEngineModel.setSettings(settings);
				renderEngineModel.generateGeneralGeometry();
				
				Map<String, Areas> map = new TreeMap<>();

				Map<String, Double> totalAreaMap = new HashMap<>();
				
				for (IfcSpace ifcSpace : model.getAllWithSubTypes(IfcSpace.class)) {
					RenderEngineInstance instance = null;
					if (ifcSpace.getRepresentation() != null) {
						instance = renderEngineModel.getInstanceFromExpressId(ifcSpace.getExpressId());
					}
					String name = getNameProperty(ifcSpace, "Name");
					if (name == null) {
						if (ifcSpace.getLongName() != null) {
							name = ifcSpace.getLongName();
						} else {
							name = "No Name";
						}
//						System.out.println(file.getName() + " - IfcSpace with no name");
					}
					String department = getNameProperty(ifcSpace, "Department");
					Double area = getDoubleProperty(ifcSpace, "Area");
					if (area == null && instance != null) {
					}
					area = instance.getArea() / 1000000.0;
					
					Areas areas = map.get(department + "_" + name);
					if (areas == null) {
						areas = new Areas(department, name);
						map.put(department + "_" + name, areas);
					}
					areas.totalArea += area;
					areas.nrAreas++;
					
					Areas totalAreas = totalmap.get(department + "_" + name);
					if (totalAreas == null) {
						totalAreas = new Areas(department, name);
						totalmap.put(department + "_" + name, totalAreas);
					}
					totalAreas.totalArea += area;
					totalAreas.nrAreas++;

					
//					dumpProperties(ifcSpace);
					
					Double totalArea = totalAreaMap.get(department);
					if (totalArea == null) {
						totalAreaMap.put(department, area);
					} else {
						totalAreaMap.put(department, totalAreaMap.get(department) + area);
					}
					
				}
				
				renderEngineModel.close();
				renderEngine.close();
				
				int row = 0;
			    Sheet sheet = wb.createSheet(file.getName());
			    
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
				if (mappings.get("Functional").contains(areas.classification)) {
					splitted.functionalm2 += areas.totalArea;
				} else if (mappings.get("Circulation").contains(areas.classification)) {
					splitted.circulationm2 += areas.totalArea;
				} else {
					splitted.otherm2 += areas.totalArea;
				}
			}
			
			Sheet totalSheet = wb.createSheet("Total");
			writeRow(totalSheet, 0, "Department", "# Areas", "Functional m2", "Circulation m2", "Other m2", "Total m2");
			int row = 2;
			for (Splitted splitted : splittedmap.values()) {
				writeRow(totalSheet, row++, splitted.department == null ? "No Department" : splitted.department, "" + splitted.nrobjects, splitted.functionalm2, splitted.circulationm2, splitted.otherm2, splitted.totalm2);
			}
			File file2 = new File(dir, "elasstic.xls");
			FileOutputStream fileOut = new FileOutputStream(file2);
			wb.write(fileOut);
			wb.close();
			fileOut.close();
		} catch (PluginException e) {
			e.printStackTrace();
		} catch (DeserializeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidFormatException e) {
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
	
	public String getNameProperty(IfcSpace ifcObject, String name) {
		for (IfcRelDefines ifcRelDefines : ifcObject.getIsDefinedBy()) {
			if (ifcRelDefines instanceof IfcRelDefinesByProperties) {
				IfcRelDefinesByProperties ifcRelDefinesByProperties = (IfcRelDefinesByProperties)ifcRelDefines;
				IfcPropertySetDefinition relatingPropertyDefinition = ifcRelDefinesByProperties.getRelatingPropertyDefinition();
				if (relatingPropertyDefinition instanceof IfcPropertySet) {
					IfcPropertySet ifcPropertySet = (IfcPropertySet)relatingPropertyDefinition;
					for (IfcProperty ifcProperty : ifcPropertySet.getHasProperties()) {
						if (ifcProperty.getName().equals(name)) {
							if (ifcProperty instanceof IfcPropertySingleValue) {
								IfcPropertySingleValue ifcPropertySingleValue = (IfcPropertySingleValue)ifcProperty;
								IfcValue value = ifcPropertySingleValue.getNominalValue();
								if (value instanceof IfcText) {
									IfcText ifcText = (IfcText)value;
									return ifcText.getWrappedValue();
								} else if (value instanceof IfcIdentifier) {
									IfcIdentifier ifcIdentifier = (IfcIdentifier)value;
									return ifcIdentifier.getWrappedValue();
								} else if (value instanceof IfcLabel) {
									IfcLabel ifcIdentifier = (IfcLabel)value;
									return ifcIdentifier.getWrappedValue();
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	public Double getDoubleProperty(IfcSpace ifcObject, String name) {
		for (IfcRelDefines ifcRelDefines : ifcObject.getIsDefinedBy()) {
			if (ifcRelDefines instanceof IfcRelDefinesByProperties) {
				IfcRelDefinesByProperties ifcRelDefinesByProperties = (IfcRelDefinesByProperties)ifcRelDefines;
				IfcPropertySetDefinition relatingPropertyDefinition = ifcRelDefinesByProperties.getRelatingPropertyDefinition();
				if (relatingPropertyDefinition instanceof IfcPropertySet) {
					IfcPropertySet ifcPropertySet = (IfcPropertySet)relatingPropertyDefinition;
					for (IfcProperty ifcProperty : ifcPropertySet.getHasProperties()) {
						if (ifcProperty.getName().equals(name)) {
							if (ifcProperty instanceof IfcPropertySingleValue) {
								IfcPropertySingleValue ifcPropertySingleValue = (IfcPropertySingleValue)ifcProperty;
								IfcValue value = ifcPropertySingleValue.getNominalValue();
								if (value instanceof IfcAreaMeasure) {
									IfcAreaMeasure ifcText = (IfcAreaMeasure)value;
									return ifcText.getWrappedValue();
								} else if (value instanceof IfcLengthMeasure) {
									return ((IfcLengthMeasure)value).getWrappedValue();
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
}