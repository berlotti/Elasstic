package org.bimserver.elasstic;

import java.io.ByteArrayOutputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.bimserver.interfaces.objects.SActionState;
import org.bimserver.interfaces.objects.SExtendedData;
import org.bimserver.interfaces.objects.SExtendedDataSchema;
import org.bimserver.interfaces.objects.SFile;
import org.bimserver.interfaces.objects.SInternalServicePluginConfiguration;
import org.bimserver.interfaces.objects.SLongActionState;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProgressTopicType;
import org.bimserver.models.log.AccessMethod;
import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.models.store.ServiceDescriptor;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.models.store.Trigger;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginContext;
import org.bimserver.plugins.PluginException;
import org.bimserver.plugins.PluginManager;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.plugins.services.NewRevisionHandler;
import org.bimserver.plugins.services.ServicePlugin;
import org.bimserver.shared.PublicInterfaceNotFoundException;
import org.bimserver.shared.exceptions.ServerException;
import org.bimserver.shared.exceptions.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ElassticTestService extends ServicePlugin {
	private static final Logger LOGGER = LoggerFactory.getLogger(ElassticTestService.class);
	private boolean initialized;
	private PluginContext pluginContext;

	public abstract String getCsvFileName();
	
	@Override
	public void init(PluginManager pluginManager) throws PluginException {
		super.init(pluginManager);
		pluginContext = pluginManager.getPluginContext(this);
		initialized = true;
	}
	
	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public ObjectDefinition getSettingsDefinition() {
		return null;
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public String getTitle() {
		return getDefaultName();
	}

	@Override
	public void register(long uoid, SInternalServicePluginConfiguration internalService, PluginConfiguration pluginConfiguration) {
		ServiceDescriptor serviceDescriptor = StoreFactory.eINSTANCE.createServiceDescriptor();
		serviceDescriptor.setProviderName("BIMserver");
		serviceDescriptor.setIdentifier("" + internalService.getOid());
		serviceDescriptor.setName(getDefaultName());
		serviceDescriptor.setDescription(getDefaultName());
		serviceDescriptor.setNotificationProtocol(AccessMethod.INTERNAL);
		serviceDescriptor.setTrigger(Trigger.NEW_REVISION);
		serviceDescriptor.setReadRevision(true);
		serviceDescriptor.setWriteExtendedData("http://bimserver.org/3dvisualizationeffects");
		registerNewRevisionHandler(uoid, serviceDescriptor, new NewRevisionHandler() {
			@Override
			public void newRevision(BimServerClientInterface bimServerClientInterface, long poid, long roid, String userToken, long soid, SObjectType settings) throws ServerException, UserException {
				try {
					Date startDate = new Date();
					Long topicId = bimServerClientInterface.getRegistry().registerProgressOnRevisionTopic(SProgressTopicType.RUNNING_SERVICE, poid, roid, getTitle());
					SLongActionState state = new SLongActionState();
					state.setTitle(getDefaultName());
					state.setState(SActionState.STARTED);
					state.setProgress(-1);
					state.setStart(startDate);
					bimServerClientInterface.getRegistry().updateProgressTopic(topicId, state);
					
					SExtendedDataSchema extendedDataSchemaByNamespace = bimServerClientInterface.getBimsie1ServiceInterface().getExtendedDataSchemaByNamespace(
							"http://bimserver.org/3dvisualizationeffects");

					SFile file = new SFile();

					SExtendedData extendedData = new SExtendedData();
					extendedData.setTitle(getTitle());
					file.setFilename(getCsvFileName());
					extendedData.setSchemaId(extendedDataSchemaByNamespace.getOid());
					try {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						IOUtils.copy(pluginContext.getResourceAsInputStream("input/" + getCsvFileName()), baos);
						file.setData(baos.toByteArray());
						file.setMime("text/json");

						long fileId = bimServerClientInterface.getServiceInterface().uploadFile(file);
						extendedData.setFileId(fileId);

						bimServerClientInterface.getBimsie1ServiceInterface().addExtendedDataToRevision(roid, extendedData);
					} catch (Exception e) {
						LOGGER.error("", e);
					}
					
					state = new SLongActionState();
					state.setProgress(100);
					state.setTitle(getDefaultName());
					state.setState(SActionState.FINISHED);
					state.setStart(startDate);
					state.setEnd(new Date());
					bimServerClientInterface.getRegistry().updateProgressTopic(topicId, state);
					
					bimServerClientInterface.getRegistry().unregisterProgressTopic(topicId);
				} catch (PublicInterfaceNotFoundException e) {
					LOGGER.error("", e);
				}
			}
		});
	}

	@Override
	public void unregister(SInternalServicePluginConfiguration internalService) {
	}
}
