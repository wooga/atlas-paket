/*
 * Copyright 2018 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.paket.base.repository.internal

import com.google.common.collect.Lists
import groovy.transform.EqualsAndHashCode
import org.gradle.api.artifacts.repositories.AuthenticationContainer
import org.gradle.api.internal.artifacts.repositories.AuthenticationSupporter
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.internal.reflect.Instantiator
import wooga.gradle.paket.base.repository.NugetArtifactRepository

@EqualsAndHashCode(includeFields=true)
class DefaultNugetArtifactRepository implements NugetArtifactRepository {

    private final FileResolver fileResolver
    private Object url
    private List<Object> dirs = new ArrayList<Object>()
    private String name

    @Delegate
    private final AuthenticationSupporter delegate

    DefaultNugetArtifactRepository(FileResolver fileResolver,
                                   Instantiator instantiator,
                                   AuthenticationContainer authenticationContainer) {
        this.delegate = new AuthenticationSupporter(instantiator, authenticationContainer)
        this.fileResolver = fileResolver
    }

    @Input
    @Optional
    @Override
    URI getUrl() {
        return url == null ? null : fileResolver.resolveUri(url)
    }

    @Override
    void setUrl(URI url) {
        this.url = url
    }

    @Override
    void setUrl(Object url) {
        this.url = url
    }

    @Input
    @Override
    Set<File> getDirs() {
        return fileResolver.resolveFiles(dirs).getFiles()
    }

    @Override
    void setDirs(Set<File> dirs) {
        assertURL()
        setDirs((Iterable<?>) dirs)
    }

    @Override
    void setDirs(Iterable<?> dirs) {
        assertURL()
        this.dirs = Lists.newArrayList(dirs)
    }

    @Override
    void dir(Object dir) {
        assertURL()
        dirs(dir)
    }

    @Override
    void dirs(Object... dirs) {
        assertURL()
        this.dirs.addAll(Arrays.asList(dirs))
    }

    private assertURL() {
        if(url != null)
        {
            throw new Exception("url already set")
        }
    }

    @Override
    String getName() {
        return name
    }

    @Override
    void setName(String name) {
        this.name = name
    }
}
