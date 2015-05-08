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
import org.bimserver.plugins.renderengine.RenderEngineGeometry;
import org.bimserver.plugins.renderengine.RenderEngineInstance;
import org.bimserver.plugins.renderengine.RenderEngineModel;
import org.bimserver.plugins.renderengine.RenderEnginePlugin;
import org.bimserver.plugins.renderengine.RenderEngineSettings;

public class Calculator {

	private Map<String, List<String>> mappings;
	private DeserializerPlugin ifcDeserializerPlugin;
	private PluginManager pluginManager;
	private RenderEnginePlugin renderEnginePlugin;

	public Calculator(String[] args) {
		pluginManager = LocalDevSetup.setupPluginManager(args);
		try {
			ifcDeserializerPlugin = pluginManager.getFirstDeserializer("ifc", Schema.IFC2X3TC1, true);
			renderEnginePlugin = pluginManager.getRenderEngine("org.bimserver.ifcengine.JvmRenderEnginePlugin", true);
		} catch (PluginException e) {
			e.printStackTrace();
		}
	}
	
	public Map<String, List<String>> getMappings() {
		return mappings;
	}
	
	protected List<Space> calculateSpaces(File file) {
		List<Space> spaces = new ArrayList<>();
		try {
			mappings = new HashMap<>();
			
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
			
			Deserializer ifcDeserializer  = ifcDeserializerPlugin.createDeserializer(new PluginConfiguration());
			ifcDeserializer.init(pluginManager.getMetaDataManager().getPackageMetaData("ifc2x3tc1"));
			IfcModelInterface model = ifcDeserializer.read(file);
			
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
				spaces.add(space);
				space.setName(ifcSpace.getName());
				space.setGuid(ifcSpace.getGlobalId());

				RenderEngineInstance instance = null;
				
				double m2fromengine = 0;
				
				if (ifcSpace.getRepresentation() != null) {
					instance = renderEngineModel.getInstanceFromExpressId(ifcSpace.getExpressId());
					RenderEngineGeometry generateGeometry = instance.generateGeometry();
					int[] indices = generateGeometry.getIndices();
					float[] normals = generateGeometry.getNormals();
					float[] vertices = generateGeometry.getVertices();
					
					float min = Float.MAX_VALUE;
					
					for (int i=0; i<vertices.length; i+=3) {
						float y = vertices[i+2];
						if (y < min) {
							min = y;
						}
					}
					space.setLowestLevel(min);

					for (int i=0; i<indices.length; i+=3) {
						
						int index1 = indices[i];
						int index2 = indices[i+1];
						int index3 = indices[i+2];
						
						if (normals[index1 * 3 + 2] == 1f && normals[index2 * 3 + 2] == 1f && normals[index3 * 3 + 2] == 1f) {
							float x1 = vertices[index1 * 3];
							float y1 = vertices[index1 * 3 + 1];
							float z1 = vertices[index1 * 3 + 2];
							float x2 = vertices[index2 * 3];
							float y2 = vertices[index2 * 3 + 1];
							float z2 = vertices[index2 * 3 + 2];
							float x3 = vertices[index3 * 3];
							float y3 = vertices[index3 * 3 + 1];
							float z3 = vertices[index3 * 3 + 2];
							
							float a = (float) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2) + Math.pow((z1 - z2), 2));
							float b = (float) Math.sqrt(Math.pow((x2 - x3), 2) + Math.pow((y2 - y3), 2) + Math.pow((z2 - z3), 2));
							float c = (float) Math.sqrt(Math.pow((x3 - x1), 2) + Math.pow((y3 - y1), 2) + Math.pow((z3 - z1), 2));
							float s = 0.5f * (a + b + c);
							float operand = s*(s-a)*(s-b)*(s-c);
							if (operand > 0) {
								m2fromengine += Math.sqrt(operand) / 1000000f;
							}
						}
					}
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
				
				if (area == null) {
					space.setArea(m2fromengine);
				} else {
					space.setArea(area);
				}

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
	    		double d = (Double)value;
	    		if (d < 0.0001) {
	    			d = 0;
	    		}
	    		cell.setCellType(Cell.CELL_TYPE_NUMERIC);
	    		cell.setCellValue(d);
	    	} else if (value instanceof Float) {
	    		float f = (Float)value;
	    		if (f < 0.0001) {
	    			f = 0;
	    		}
	    		cell.setCellType(Cell.CELL_TYPE_NUMERIC);
	    		cell.setCellValue(f);
	    	} else if (value instanceof Integer) {
	    		cell.setCellType(Cell.CELL_TYPE_NUMERIC);
	    		cell.setCellValue((Integer)value);
	    	} else if (value == null) {
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