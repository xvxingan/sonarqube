/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.plugins;

import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.UriReader;
import org.sonar.updatecenter.common.UpdateCenter;
import org.sonar.updatecenter.common.Version;

import java.net.URI;
import java.net.URISyntaxException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class UpdateCenterClientTest {

  private UpdateCenterClient client;
  private UriReader reader;
  private static final String BASE_URL = "http://update.sonarsource.org";

  @Before
  public void startServer() throws Exception {
    reader = mock(UriReader.class);
    Settings settings = new Settings().setProperty(UpdateCenterClient.URL_PROPERTY, BASE_URL);
    client = new UpdateCenterClient(reader, settings);
  }

  @Test
  public void downloadUpdateCenter() throws URISyntaxException {
    when(reader.readString(new URI(BASE_URL), Charsets.UTF_8)).thenReturn("sonar.versions=2.2,2.3");
    UpdateCenter center = client.getCenter();
    verify(reader, times(1)).readString(new URI(BASE_URL), Charsets.UTF_8);
    assertThat(center.getSonar().getVersions()).containsOnly(Version.create("2.2"), Version.create("2.3"));
    assertThat(client.getLastRefreshDate()).isNotNull();
  }

  @Test
  public void not_available_before_initialization() {
    assertThat(client.getLastRefreshDate()).isNull();
  }

  @Test
  public void ignore_connection_errors() {
    when(reader.readString(any(URI.class), eq(Charsets.UTF_8))).thenThrow(new SonarException());
    assertThat(client.getCenter()).isNull();
  }


  @Test
  public void cache_data() throws Exception {
    when(reader.readString(new URI(BASE_URL), Charsets.UTF_8)).thenReturn("sonar.versions=2.2,2.3");

    client.getCenter();
    client.getCenter();

    verify(reader, times(1)).readString(new URI(BASE_URL), Charsets.UTF_8);
  }

  @Test
  public void forceRefresh() throws Exception {
    when(reader.readString(new URI(BASE_URL), Charsets.UTF_8)).thenReturn("sonar.versions=2.2,2.3");

    client.getCenter();
    client.getCenter(true);

    verify(reader, times(2)).readString(new URI(BASE_URL), Charsets.UTF_8);
  }
}