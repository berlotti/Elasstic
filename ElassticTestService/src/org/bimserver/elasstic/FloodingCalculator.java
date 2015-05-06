package org.bimserver.elasstic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.bimserver.LocalDevSetup;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.emf.Schema;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
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

public class FloodingCalculator extends Calculator {
    NumberFormat formatter = new DecimalFormat("#0.00");     

	public static void main(String[] args) {
		new FloodingCalculator().start(args);
	}

	private void start(String[] args) {
		float level = 500.0f;
		
		PluginManager pluginManager = LocalDevSetup.setupPluginManager(args);
		try {
			DeserializerPlugin ifcDeserializerPlugin = pluginManager.getFirstDeserializer("ifc", Schema.IFC2X3TC1, true);
			File dir = new File("E:\\elasticlastfiles");
			
			Workbook wb = new HSSFWorkbook();
			Sheet sheet = wb.createSheet("Flooding");

			int row = 0;
			writeRow(sheet, row++, "GUID", "Name", "File", "Elevation (m)");
			row++;
			
			for (File file : dir.listFiles()) {
				if (!file.getName().endsWith(".ifc")) {
					continue;
				}
				System.out.println(file.getName());
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
				
				for (IfcSpace ifcSpace : model.getAll(IfcSpace.class)) {
					RenderEngineInstance renderEngineInstance = renderEngineModel.getInstanceFromExpressId(ifcSpace.getExpressId());
					RenderEngineGeometry generateGeometry = renderEngineInstance.generateGeometry();
					float[] vertices = generateGeometry.getVertices();
					
					float min = Float.MAX_VALUE;
					
					for (int i=0; i<vertices.length; i+=3) {
						float y = vertices[i+2];
						if (y < min) {
							min = y;
						}
					}
					writeRow(sheet, row++, ifcSpace.getGlobalId(), ifcSpace.getName(), file.getName(), min / 1000);
				}
				
				renderEngineModel.close();
				renderEngine.close();
			}
			
			File file2 = new File(dir, "flooding.xls");
			FileOutputStream fileOut = new FileOutputStream(file2);
			wb.write(fileOut);
			wb.close();
			fileOut.close();
		} catch (PluginException e) {
			e.printStackTrace();
		} catch (DeserializeException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
