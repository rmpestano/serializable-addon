package org.example.commands;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.Projects;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.ui.command.AbstractUICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;

public class AddSerializableCommand extends AbstractUICommand {

	@Inject
	ProjectFactory factory;

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(AddSerializableCommand.class)
				.name("serialize").category(Categories.create("Forge")).description("Makes all classes serializable for project "+getProject(context).getFacet(MetadataFacet.class).getProjectName());
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
		Project project = getProject(context.getUIContext());
		Resource<?> root = project.getRoot();
		List<FileResource<?>> javaFiles = new ArrayList<>();
		findProjectClasses(root,javaFiles);
		int modifiedClasses = 0;
		for (FileResource<?> javaFile : javaFiles) {
			JavaSource<?> source = (JavaSource<?>) Roaster.parse(javaFile.getUnderlyingResourceObject());
			if(source.isClass()){
				JavaClassSource javaClass = (JavaClassSource) source;
				if(!javaClass.hasInterface(Serializable.class)){
					modifiedClasses ++;
					javaClass.addInterface(Serializable.class);
					javaClass.addField().setPrivate().setStatic(true).setFinal(true).setName("serialVersionUID").setType("long")
							.setLiteralInitializer("1L");
					JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);
					facet.saveJavaSource(javaClass);
				}
			}

		}
 		return Results.success(modifiedClasses > 0 ? project.getFacet(MetadataFacet.class).getProjectName() +": "+modifiedClasses+" classes are now serializable":"All classes are already serializable");
	}

	private Project getProject(UIContext context) {
		return Projects.getSelectedProject(factory,context);
	}

	private void findProjectClasses(Resource<?> root,List<FileResource<?>> classesFound) {
			for (Resource<?> resource : root.listResources()) {
				if (resource instanceof DirectoryResource) {
					findProjectClasses(resource, classesFound);
				}
				//for each class in current dir verifies if its a java class
				if (resource.getName().endsWith(".java")) {
					classesFound.add(resource.reify(FileResource.class));
				}
			}
	}

	@Override
	public boolean isEnabled(UIContext context) {
		Project project = Projects.getSelectedProject(factory,context);
		return project != null;
	}


}