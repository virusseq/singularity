/*
 * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE archive_status as enum ('BUILDING', 'COMPLETE', 'FAILED');

CREATE TABLE if not exists archive_meta
(
    archive_id          uuid      not null,
    num_of_downloads    int       not null DEFAULT 0,
    num_of_samples      int       not null,
    PRIMARY KEY (archive_id)
);

CREATE TABLE if not exists archive_all
(
    id       uuid     REFERENCES archive_meta(archive_id) ON DELETE CASCADE  DEFAULT uuid_generate_v4(),
    status              archive_status      not null,
    timestamp           bigint              unique,
    object_id           uuid,
    PRIMARY KEY (id)
);

CREATE TABLE if not exists archive_set_query
(
    id        uuid    REFERENCES archive_meta(archive_id) ON DELETE CASCADE  DEFAULT uuid_generate_v4(),
    status              archive_status      not null,
    timestamp           bigint              not null,
    set_query_hash      VARCHAR             unique,
    object_id           uuid,
    PRIMARY KEY (id)
);
