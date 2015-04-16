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
		public int totalArea;
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
				
				Map<String, Areas> map = new HashMap<>();
				Set<String> departments = new HashSet<>();
				
				for (IfcSpace ifcSpace : model.getAllWithSubTypes(IfcSpace.class)) {
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
					if (area != null) {
						areas.totalArea += area;
					} else {
//						dumpProperties(ifcSpace);
//						System.out.println(file.getName() + " - IfcSpace with no area " + ifcSpace.getGlobalId());
					}
//					dumpProperties(ifcSpace);
					areas.nrAreas++;
				}
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