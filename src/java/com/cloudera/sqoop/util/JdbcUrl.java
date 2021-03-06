/**
 * Licensed to Cloudera, Inc. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Cloudera, Inc. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.sqoop.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Some utilities for parsing JDBC URLs which may not be tolerated by Java's
 * java.net.URL class. java.net.URL does not support multi:part:scheme://
 * components, which virtually all JDBC connect string URLs have.
 */
public final class JdbcUrl {

  public static final Log LOG = LogFactory.getLog(JdbcUrl.class.getName());

  private JdbcUrl() {
  }

  /**
   * @return the database name from the connect string, which is typically the
   *         'path' component, or null if we can't.
   */
  public static String getDatabaseName(String connectString) {
    try {
      String sanitizedString = null;
      int schemeEndOffset = connectString.indexOf("://");
      if (-1 == schemeEndOffset) {
        // couldn't find one? try our best here.
        sanitizedString = "http://" + connectString;
        LOG.warn("Could not find database access scheme in connect string "
            + connectString);
      } else {
        sanitizedString = "http" + connectString.substring(schemeEndOffset);
      }

      URL connectUrl = new URL(sanitizedString);
      String databaseName = connectUrl.getPath();
      if (null == databaseName) {
        return null;
      }

      // This is taken from a 'path' part of a URL, which may have leading '/'
      // characters; trim them off.
      while (databaseName.startsWith("/")) {
        databaseName = databaseName.substring(1);
      }

      return databaseName;
    } catch (MalformedURLException mue) {
      LOG.error("Malformed connect string URL: " + connectString
          + "; reason is " + mue.toString());
      return null;
    }
  }

  /**
   * @return the hostname from the connect string, or null if we can't.
   */
  public static String getHostName(String connectString) {
    try {
      String sanitizedString = null;
      int schemeEndOffset = connectString.indexOf("://");
      if (-1 == schemeEndOffset) {
        // Couldn't find one? ok, then there's no problem, it should work as a
        // URL.
        sanitizedString = connectString;
      } else {
        sanitizedString = "http" + connectString.substring(schemeEndOffset);
      }

      URL connectUrl = new URL(sanitizedString);
      return connectUrl.getHost();
    } catch (MalformedURLException mue) {
      LOG.error("Malformed connect string URL: " + connectString
          + "; reason is " + mue.toString());
      return null;
    }
  }

  /**
   * @return the port from the connect string, or -1 if we can't.
   */
  public static int getPort(String connectString) {
    try {
      String sanitizedString = null;
      int schemeEndOffset = connectString.indexOf("://");
      if (-1 == schemeEndOffset) {
        // Couldn't find one? ok, then there's no problem, it should work as a
        // URL.
        sanitizedString = connectString;
      } else {
        sanitizedString = "http" + connectString.substring(schemeEndOffset);
      }

      URL connectUrl = new URL(sanitizedString);
      return connectUrl.getPort();
    } catch (MalformedURLException mue) {
      LOG.error("Malformed connect string URL: " + connectString
          + "; reason is " + mue.toString());
      return -1;
    }
  }

  public static String getConnectionUrl(String connectString) {
    try {
      URL connectUrl = makeUrlFromJdbcString(connectString);
      String queryStr = connectUrl.getQuery();
      if (null != queryStr) {
        return connectString.substring(0,
            connectString.length() - queryStr.length() - 1);
      } else {
        return connectString;
      }
    } catch (MalformedURLException mue) {
      LOG.error("Malformed connect string URL: " + connectString
          + "; reason is " + mue.toString());
      return connectString;
    }
  }

  public static Properties getConnectionProperties(String connectString,
      String username, String password) {
    Properties props = new Properties();

    // Add any query parameters as properties.
    try {
      URL connectUrl = makeUrlFromJdbcString(connectString);
      String queryStr = connectUrl.getQuery();
      if (null != queryStr) {
        String[] params = queryStr.split("&");
        for (String param : params) {
          String[] keyValue = param.split("=");
          try {
            String key = URLDecoder.decode(keyValue[0], "UTF-8");
            String value = keyValue.length == 1 ? "" : URLDecoder.decode(
                keyValue[1], "UTF-8");
            props.put(key, value);
          } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Impossible exception", e);
          }
        }
      }
    } catch (MalformedURLException mue) {
      LOG.error("Malformed connect string URL: " + connectString
          + "; reason is " + mue.toString());
    }

    // Add username and password.
    if (null != username) {
      props.put("user", username);
      props.put("password", password);
    }

    return props;
  }

  /**
   * Convert a JDBC-compliant connection string into a URL that we can use to
   * extract pieces of the connection (hostname, port, query, etc)
   * 
   * @param connectStr
   * @return URL-equivalent
   * @throws MalformedURLException
   */
  public static URL makeUrlFromJdbcString(String connectStr)
      throws MalformedURLException {
    String sanitizedString = null;
    if (connectStr.startsWith("jdbc:")) {
      sanitizedString = "http://" + connectStr.substring("jdbc:".length());
    } else if (connectStr.startsWith("http://")
        || connectStr.startsWith("https://")) {
      sanitizedString = connectStr;
    } else {
      sanitizedString = "http://" + connectStr;
    }

    return new URL(sanitizedString);
  }
}
