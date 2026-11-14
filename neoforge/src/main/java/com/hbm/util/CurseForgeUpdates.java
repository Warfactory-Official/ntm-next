/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.util;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.hbm.platform.Platform;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.jspecify.annotations.Nullable;

public final class CurseForgeUpdates {
    private CurseForgeUpdates() {}

    public static @Nullable Release check(
            URI endpoint, String minecraft, String loader, String installed)
            throws IOException, InterruptedException {
        var timeout = Duration.ofSeconds(15);
        var request =
                HttpRequest.newBuilder(endpoint)
                        .timeout(timeout)
                        .header("Accept", "application/json")
                        .header("User-Agent", "NTM-NEXT/" + installed)
                        .GET()
                        .build();
        String body;
        try (var client = HttpClient.newBuilder().connectTimeout(timeout).build()) {
            var response =
                    client.send(
                            request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IOException("CFWidget returned HTTP " + response.statusCode());
            }
            body = response.body();
        }
        var current = releaseVersion(installed);
        var minecraftTag = new JsonPrimitive(minecraft);
        var loaderTag = new JsonPrimitive(loader);
        Release update = null;
        for (var entry : JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("files")) {
            var file = entry.getAsJsonObject();
            var versions = file.getAsJsonArray("versions");
            if (!file.get("type").getAsString().equals("release")
                    || !versions.contains(minecraftTag)
                    || !versions.contains(loaderTag)) continue;
            var version = releaseVersion(file.get("display").getAsString());
            if (Platform.compareVersions(version, current) <= 0) continue;
            if (update == null || Platform.compareVersions(version, update.version()) > 0) {
                update = new Release(version, URI.create(file.get("url").getAsString()));
            }
        }
        return update;
    }

    private static String releaseVersion(String version) {
        int metadata = version.indexOf('+');
        return metadata < 0 ? version : version.substring(0, metadata);
    }

    public record Release(String version, URI url) {}
}
