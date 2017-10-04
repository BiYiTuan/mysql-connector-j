/*
  Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.

  The MySQL Connector/J is licensed under the terms of the GPLv2
  <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most MySQL Connectors.
  There are special exceptions to the terms and conditions of the GPLv2 as it is applied to
  this software, see the FOSS License Exception
  <http://www.mysql.com/about/legal/licensing/foss-exception.html>.

  This program is free software; you can redistribute it and/or modify it under the terms
  of the GNU General Public License as published by the Free Software Foundation; version 2
  of the License.

  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along with this
  program; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth
  Floor, Boston, MA 02110-1301  USA

 */

package com.mysql.cj.jdbc.interceptors;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import com.mysql.cj.api.MysqlConnection;
import com.mysql.cj.api.Query;
import com.mysql.cj.api.interceptors.QueryInterceptor;
import com.mysql.cj.api.io.ServerSession;
import com.mysql.cj.api.jdbc.JdbcConnection;
import com.mysql.cj.api.log.Log;
import com.mysql.cj.api.mysqla.result.Resultset;
import com.mysql.cj.core.exceptions.ExceptionFactory;
import com.mysql.cj.core.util.Util;
import com.mysql.cj.jdbc.util.ResultSetUtil;

public class ServerStatusDiffInterceptor implements QueryInterceptor {

    private Map<String, String> preExecuteValues = new HashMap<>();

    private Map<String, String> postExecuteValues = new HashMap<>();

    private JdbcConnection connection;

    private Log log;

    public QueryInterceptor init(MysqlConnection conn, Properties props, Log l) {
        this.connection = (JdbcConnection) conn;
        this.log = l;
        return this;
    }

    @Override
    public <T extends Resultset> T postProcess(Supplier<String> sql, Query interceptedQuery, T originalResultSet, ServerSession serverSession) {

        populateMapWithSessionStatusValues(this.postExecuteValues);

        this.log.logInfo("Server status change for query:\n" + Util.calculateDifferences(this.preExecuteValues, this.postExecuteValues));

        return null; // we don't actually modify a result set

    }

    private void populateMapWithSessionStatusValues(Map<String, String> toPopulate) {
        java.sql.Statement stmt = null;
        java.sql.ResultSet rs = null;

        try {
            try {
                toPopulate.clear();

                stmt = this.connection.createStatement();
                rs = stmt.executeQuery("SHOW SESSION STATUS");
                ResultSetUtil.resultSetToMap(toPopulate, rs);
            } finally {
                if (rs != null) {
                    rs.close();
                }

                if (stmt != null) {
                    stmt.close();
                }
            }
        } catch (SQLException ex) {
            throw ExceptionFactory.createException(ex.getMessage(), ex);
        }
    }

    public <T extends Resultset> T preProcess(Supplier<String> sql, Query interceptedQuery) {

        populateMapWithSessionStatusValues(this.preExecuteValues);

        return null; // we don't actually modify a result set
    }

    public boolean executeTopLevelOnly() {
        return true;
    }

    public void destroy() {
        this.connection = null;
        this.log = null;
    }
}
