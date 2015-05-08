package org.bimserver.elasstic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class Calculator {

	private String[] args;

	public Calculator(String[] args) {
		this.args = args;
	}
	
	protected List<Space> calculateSpaces(File file) {
		List<Space> spaces = new ArrayList<>();
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
			
			for (IfcSpace ifcSpace : model.getAllWithSubTypes(IfcSpace.class)) {
				Space space = new Space();
				space.setName(ifcSpace.getName());
				space.setGuid(ifcSpace.getGlobalId());

				RenderEngineInstance instance = null;
				if (ifcSpace.getRepresentation() != null) {
					instance = renderEngineModel.getInstanceFromExpressId(ifcSpace.getExpressId());
				}
				String classification = getNameProperty(ifcSpace, "Name");
				if (classification == null) {
					if (ifcSpace.getLongName() != null) {
						classification = ifcSpace.getLongName();
					} else {
						classification = "No Name";
					}
				}
				space.setClassification(classification);
				String department = getNameProperty(ifcSpace, "Department");
				space.setDepartment(department);
				Double area = getDoubleProperty(ifcSpace, "Area");
				if (area == null && instance != null) {
					area = instance.getArea() / 1000000.0;
				}
				space.setArea(area);
				
				if (mappings.get("Functional").contains(classification)) {
					space.setFunction("Functional");
				} else if (mappings.get("Circulation").contains(classification)) {
					space.setFunction("Circulation");
				}
			}
			
			renderEngineModel.close();
			renderEngine.close();
		} catch (PluginException e) {
			e.printStackTrace();
		} catch (DeserializeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		}

		return spaces;
	}
	
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