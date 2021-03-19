package com.squareup.wire.mojo;

import com.google.common.base.Stopwatch;
import com.google.common.io.Closer;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.squareup.wire.java.JavaGenerator;
import com.squareup.wire.schema.CoreLoader;
import com.squareup.wire.schema.PruningRules;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;
import com.squareup.wire.schema.Type;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;


/** A maven mojo that executes Wire's JavaGenerator. */
@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class WireGenerateSourcesMojo extends AbstractMojo {
  /** The root of the proto source directory. */
  @Parameter(
          property = "wire.protoSourceDirectory",
          defaultValue = "${project.basedir}/src/main/proto")
  private String protoSourceDirectory;

  @Parameter(property = "wire.protoPaths")
  private String[] protoPaths;

  @Parameter(property = "wire.includes")
  private String[] includes;

  @Parameter(property = "wire.excludes")
  private String[] excludes;

  @Parameter(property = "wire.serviceFactory")
  private String serviceFactory;

  /** List of proto files to compile relative to ${protoPaths}. */
  @Parameter(property = "wire.protoFiles", required = true)
  private String[] protoFiles;

  @Parameter(
          property = "wire.generatedSourceDirectory",
          defaultValue = "${project.build.directory}/generated-sources/wire")
  private String generatedSourceDirectory;

  @Parameter(
          defaultValue = "${project}",
          required = true,
          readonly = true)
  private MavenProject project;

  @Override public void execute() throws MojoExecutionException, MojoFailureException {
    // Add the directory into which generated sources are placed as a compiled source root.
    project.addCompileSourceRoot(generatedSourceDirectory);

    try {
      List<String> directories = protoPaths != null && protoPaths.length > 0
              ? Arrays.asList(protoPaths)
              : Collections.singletonList(protoSourceDirectory);
      List<String> protoFilesList = Arrays.asList(protoFiles);

      Schema schema = loadSchema(directories, protoFilesList);

      PruningRules identifierSet = pruningRules();
      if (!identifierSet.isEmpty()) {
        schema = retainRoots(identifierSet, schema);
      }

      JavaGenerator javaGenerator = JavaGenerator.get(schema);

      for (ProtoFile protoFile : schema.getProtoFiles()) {
        if (!protoFilesList.isEmpty() && !protoFilesList.contains(protoFile.getLocation().getPath())) {
          continue; // Don't emit anything for files not explicitly compiled.
        }

        for (Type type : protoFile.getTypes()) {
          Stopwatch stopwatch = Stopwatch.createStarted();
          TypeSpec typeSpec = javaGenerator.generateType(type);
          ClassName javaTypeName = javaGenerator.generatedTypeName(type);
          writeJavaFile(javaTypeName, typeSpec, type.getLocation().withPathOnly());
          getLog().info(String.format("Generated %s in %s", javaTypeName, stopwatch));
        }
      }
    } catch (Exception e) {
      throw new MojoExecutionException("Wire Plugin: Failure compiling proto sources.", e);
    }
  }

  private PruningRules pruningRules() {
    PruningRules.Builder identifierSetBuilder = new PruningRules.Builder();
    if (includes != null) {
      for (String identifier : includes) {
        identifierSetBuilder.addRoot(identifier);
      }
    }
    if (excludes != null) {
      for (String identifier : excludes) {
        identifierSetBuilder.prune(identifier);
      }
    }
    return identifierSetBuilder.build();
  }

  private Schema retainRoots(PruningRules identifierSet, Schema schema) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    int oldSize = countTypes(schema);

    Schema prunedSchema = schema.prune(identifierSet);
    int newSize = countTypes(prunedSchema);

    for (String rule : identifierSet.unusedRoots()) {
      getLog().warn(String.format("Unused include: %s", rule));
    }
    for (String rule : identifierSet.unusedPrunes()) {
      getLog().warn(String.format("Unused exclude: %s", rule));
    }

    getLog().info(String.format("Pruned schema from %s types to %s types in %s",
            oldSize, newSize, stopwatch));

    return prunedSchema;
  }

  private int countTypes(Schema prunedSchema) {
    int result = 0;
    for (ProtoFile protoFile : prunedSchema.getProtoFiles()) {
      result += protoFile.getTypes().size();
    }
    return result;
  }

  @SuppressWarnings("UnstableApiUsage")
  private Schema loadSchema(List<String> directories, List<String> protos) throws IOException {
    Stopwatch stopwatch = Stopwatch.createStarted();

    FileSystem fs = FileSystems.getDefault();
    SchemaLoader schemaLoader = new SchemaLoader(fs);

    List<Path> sources = directories.stream().map(fs::getPath).collect(Collectors.toList());
    Map<Path, Path> directoryPaths = directoryPaths(Closer.create(), sources);

    List<Location> allDirectories = directoryPaths.keySet().stream().map(Path::toString).map(Location::get).collect(Collectors.toList());
    List<Location> sourcePath;
    List<Location> protoPath;

    if (!protos.isEmpty()) {
      sourcePath = protos.stream().map( it -> locationOfProto(directoryPaths, it) ).collect(Collectors.toList());
      protoPath = allDirectories;
    } else {
      sourcePath = allDirectories;
      protoPath = List.of();
    }
    schemaLoader.initRoots( sourcePath, protoPath );

    Schema schema = schemaLoader.loadSchema();

    getLog().info(String.format("Loaded %s proto files in %s",
            schema.getProtoFiles().size(), stopwatch));

    return schema;
  }

  @SuppressWarnings("UnstableApiUsage")
  private static Map<Path, Path> directoryPaths(Closer closer, List<Path> sources )  {
    Map<Path, Path> directories = new HashMap<>();
    for (var source : sources) {
      if (Files.isRegularFile(source)) {
        FileSystem sourceFs;
        try {
          sourceFs = FileSystems.newFileSystem(source, WireGenerateSourcesMojo.class.getClassLoader());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        closer.register(sourceFs);
        directories.put(source, sourceFs.getRootDirectories().iterator().next());
      } else {
        directories.put(source, source);
      }
    }
    return directories;
  }

  /** Searches {@code directories} trying to resolve {@code proto}. Returns the location if it is found. */
  private static Location locationOfProto(Map<Path, Path> directories, String proto) {
    Optional<Map.Entry<Path, Path>> directoryEntry = directories.entrySet().stream().filter(d -> Files.exists(d.getValue().resolve(proto))).findFirst();

    if (directoryEntry.isEmpty()) {
      if (CoreLoader.INSTANCE.isWireRuntimeProto(proto)) {
        return Location.get(CoreLoader.WIRE_RUNTIME_JAR, proto);
      }
      throw new RuntimeException(new FileNotFoundException("Failed to locate " + proto + " in " + directories.keySet()));
    }

    return Location.get(directoryEntry.get().getKey().toString(), proto);
  }

  private void writeJavaFile(ClassName javaTypeName, TypeSpec typeSpec, Location location)
          throws IOException {
    JavaFile.Builder builder = JavaFile.builder(javaTypeName.packageName(), typeSpec)
            .addFileComment("$L", "Code generated by Wire protocol buffer compiler, do not edit.");
    if (location != null) {
      builder.addFileComment("\nSource file: $L", location);
    }
    JavaFile javaFile = builder.build();
    try {
      javaFile.writeTo(new File(generatedSourceDirectory));
    } catch (IOException e) {
      throw new IOException("Failed to write " + javaFile.packageName + "."
              + javaFile.typeSpec.name + " to " + generatedSourceDirectory, e);
    }
  }
}
