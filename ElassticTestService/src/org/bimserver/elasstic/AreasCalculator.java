package org.bimserver.elasstic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
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

import au.com.bytecode.opencsv.CSVWriter;

import com.google.common.base.Charsets;

public class AreasCalculator {
	public static void main(String[] args) {
		new AreasCalculator().start(args);
	}
	
	private class Areas {
		public Areas(String name) {
			this.name = name;
		}
		public String name;
		public int nrAreas;
		public double totalArea;
	}

	private void start(String[] args) {
		PluginManager pluginManager = LocalDevSetup.setupPluginManager(args);
		try {
			DeserializerPlugin ifcDeserializerPlugin = pluginManager.getFirstDeserializer("ifc", Schema.IFC2X3TC1, true);
			File dir = new File("E:\\elasticlastfiles");
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
				
				Map<String, Areas> map = new HashMap<>();
				Set<String> departments = new HashSet<>();

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
					if (department == null) {
//						System.out.println(file.getName() + " - " + "No Department " + ifcSpace.getGlobalId());
					} else {
						departments.add(department);
					}
					Double area = getDoubleProperty(ifcSpace, "Area");
					Areas areas = map.get(name);
					if (areas == null) {
						areas = new Areas(name);
						areas.name = name;
						map.put(name, areas);
					}
					if (area == null && instance != null) {
						area = instance.getArea() / 1000000.0;
					}
					areas.totalArea += area;
//					dumpProperties(ifcSpace);
					
					Double totalArea = totalAreaMap.get(department);
					if (totalArea == null) {
						totalAreaMap.put(department, area);
					} else {
						totalAreaMap.put(department, totalAreaMap.get(department) + area);
					}
					
					areas.nrAreas++;
				}
				
				renderEngineModel.close();
				renderEngine.close();
				
				System.out.println(file.getName() + " - " + StringUtils.join(departments, " "));
				CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(new FileOutputStream(new File(dir, file.getName() + ".csv")), Charsets.UTF_8));
				csvWriter.writeNext(new String[]{
					"Classification",
					"# Areas",
					"Total m2"
				});
				for (Areas areas : map.values()) {
					csvWriter.writeNext(new String[]{
						areas.name, "" + areas.nrAreas, "" + areas.totalArea
					});
				}
				csvWriter.writeNext(new String[]{});
				csvWriter.close();
			}
		} catch (PluginException e) {
			e.printStackTrace();
		} catch (DeserializeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Double calculateArea(IfcSpace ifcSpace) {
		IfcProductRepresentation representation = ifcSpace.getRepresentation();
		for (IfcRepresentation ifcRepresentation : representation.getRepresentations()) {
			for (IfcRepresentationItem ifcRepresentationItem : ifcRepresentation.getItems()) {
				if (ifcRepresentationItem instanceof IfcExtrudedAreaSolid) {
					IfcExtrudedAreaSolid ifcExtrudedAreaSolid = (IfcExtrudedAreaSolid)ifcRepresentationItem;
					IfcProfileDef ifcProfileDef = ifcExtrudedAreaSolid.getSweptArea();
					if (ifcProfileDef instanceof IfcRectangleProfileDef) {
						IfcRectangleProfileDef ifcRectangleProfileDef = (IfcRectangleProfileDef)ifcProfileDef;
						return ((ifcRectangleProfileDef.getXDim() / 1000.0) * (ifcRectangleProfileDef.getYDim() / 1000.0));
					} else {
						System.out.println("Unimplemented: " + ifcProfileDef);
					}
				} else {
					System.out.println("Unimplemented: " + ifcRepresentationItem);
				}
			}
		}
		return null;
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