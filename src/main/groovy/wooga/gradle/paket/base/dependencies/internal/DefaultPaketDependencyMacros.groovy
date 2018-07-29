/*
 * Copyright 2017 the original author or authors.
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

package wooga.gradle.paket.base.dependencies.internal

import org.gradle.util.GUtil
import wooga.gradle.paket.base.dependencies.PaketDependencyMacros

class DefaultPaketDependencyMacros implements PaketDependencyMacros {

    private final Set<String> frameworks = new HashSet<String>()

    @Override
    List<String> getFrameworks() {
        List<String> result = new ArrayList<String>()
        for (String framework in this.frameworks) {
            result.add(framework)
        }
        result
    }

    @Override
    void setFrameworks(Iterable<String> frameworks) {
        this.frameworks.clear()
        GUtil.addToCollection(this.frameworks, frameworks)
    }

    @Override
    void frameworks(String... frameworks) {
        if (frameworks == null) {
            throw new IllegalArgumentException("frameworks == null!")
        }
        this.frameworks.addAll(Arrays.asList(frameworks))
    }

    @Override
    void frameworks(Iterable<String> frameworks) {
        GUtil.addToCollection(this.frameworks, frameworks)
    }

    @Override
    void framework(String framework) {
        frameworks(framework)
    }
}
