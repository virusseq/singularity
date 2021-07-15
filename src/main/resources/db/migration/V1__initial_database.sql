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

CREATE TYPE archive_type as enum ('ALL', 'SET_QUERY');

CREATE TABLE if not exists archive
(
    id              uuid             NOT NULL DEFAULT uuid_generate_v4(),
    status          archive_status   NOT NULL,
    created_at      bigint      NOT NULL DEFAULT extract(epoch from now()),
    hash_info       VARCHAR          NOT NULL CHECK (hash_info <> ''),
    hash            VARCHAR          NOT NULL UNIQUE CHECK (hash <> ''),
    type            archive_type     NOT NULL,
    num_of_samples      int          NOT NULL,
    num_of_downloads    int          NOT NULL DEFAULT 0,
    object_id           uuid,
    PRIMARY KEY (id)
);

CREATE FUNCTION add_hash() RETURNS trigger AS $add_hash$
    BEGIN
        -- Check that empname and salary are given
        IF NEW.hash_info IS NULL THEN
            RAISE EXCEPTION 'hash_info cannot be null';
        END IF;

        -- Update the hash
        NEW.hash := MD5(NEW.hash_info);
        RETURN NEW;
    END;
$add_hash$ LANGUAGE plpgsql;

CREATE TRIGGER add_hash BEFORE INSERT OR UPDATE ON archive
    FOR EACH ROW EXECUTE PROCEDURE add_hash();
