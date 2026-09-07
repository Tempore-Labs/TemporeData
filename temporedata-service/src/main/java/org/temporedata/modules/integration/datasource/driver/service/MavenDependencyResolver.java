package org.temporedata.modules.integration.datasource.driver.service;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.temporedata.api.base.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Resolves a Maven GAV (groupId:artifactId:version) into its artifact plus all
 * transitive dependencies and downloads them into a local cache directory.
 * Built on Maven Resolver (Eclipse Aether package namespace).
 */
@Slf4j
@Service
public class MavenDependencyResolver {

    private static final String DEFAULT_REPOS =
            "https://maven.aliyun.com/repository/public";

    private final RepositorySystem repositorySystem = new RepositorySystemSupplier().get();

    @Value("${temporedata.plugin.maven.repos:" + DEFAULT_REPOS + "}")
    private String repoUrls;

    @Value("${temporedata.plugin.maven.cache:}")
    private String cacheOverride;

    /**
     * Resolve {@code group:artifact:version} plus its transitive dependencies.
     *
     * @return list of dependency jar files (never empty; throws on failure)
     */
    public List<File> resolve(String group, String artifact, String version) {
        String gav = group + ":" + artifact + ":" + version;
        try {
            String cacheDir = cacheDir();
            org.eclipse.aether.DefaultRepositorySystemSession session =
                    new org.eclipse.aether.DefaultRepositorySystemSession();
            session.setConfigProperty("aether.connector.basic.connectTimeout", 10000);
            session.setConfigProperty("aether.connector.basic.requestTimeout", 15000);
            LocalRepository localRepository = new LocalRepository(cacheDir);
            session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository));

            CollectRequest collect = new CollectRequest(
                    new Dependency(new DefaultArtifact(gav), "compile"),
                    repositorySystem.newResolutionRepositories(session, buildRepositories()));
            DependencyNode root;
            try {
                root = repositorySystem.collectDependencies(session, collect).getRoot();
            } catch (DependencyCollectionException e) {
                log.error("Dependency collect failed for {}", gav, e);
                throw new BusinessException("解析依赖失败: " + gav + " -> " + collectCauses(e));
            }

            DependencyResult result;
            try {
                result = repositorySystem.resolveDependencies(session, new DependencyRequest(root, null));
            } catch (DependencyResolutionException e) {
                throw new BusinessException("下载依赖失败: " + gav + " -> " + e.getMessage());
            }

            List<File> files = result.getArtifactResults().stream()
                    .map(r -> r.getArtifact() == null ? null : r.getArtifact().getFile())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (files.isEmpty()) {
                throw new BusinessException("未解析到任何依赖: " + gav);
            }
            log.info("Resolved {} dependency files for {}", files.size(), gav);
            return files;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Maven dependency resolve failed for {}", gav, e);
            throw new BusinessException("依赖解析异常: " + gav);
        }
    }

    private List<RemoteRepository> buildRepositories() {
        List<RemoteRepository> repos = new ArrayList<>();
        int index = 0;
        if (repoUrls != null) {
            for (String url : repoUrls.split(",")) {
                url = url.trim();
                if (!url.isEmpty()) {
                    repos.add(new RemoteRepository.Builder("repo" + (index++), "default", url).build());
                }
            }
        }
        if (repos.isEmpty()) {
            repos.add(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build());
        }
        return repos;
    }

    private String cacheDir() {
        if (cacheOverride != null && !cacheOverride.trim().isEmpty()) {
            return cacheOverride.trim();
        }
        // fall back to a writable temp dir (avoid ~/.temporedata EPERM in sandboxes)
        return Paths.get(System.getProperty("java.io.tmpdir"), "temporedata-plugins").toString();
    }

    private static String collectCauses(DependencyCollectionException e) {
        List<Exception> exceptions = e.getResult() != null ? e.getResult().getExceptions() : java.util.Collections.emptyList();
        if (exceptions == null || exceptions.isEmpty()) {
            return e.getMessage() == null ? "" : e.getMessage();
        }
        return exceptions.stream()
                .map(Throwable::getMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" | "));
    }
}