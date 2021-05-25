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

package org.cancogenvirusseq.all.health;

import java.util.Optional;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.cancogenvirusseq.all.components.Files;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

@Component
@RequiredArgsConstructor
public class UpdateFileBundleHealthCheck implements HealthIndicator {

  private static final String MESSAGE_KEY = "updateFileBundleDisposable";
  private final Files files;

  @Override
  public Health health() {
    return Optional.of(isDisposableRunning.test(files.getUpdateFileBundleDisposable()))
        .filter(Boolean::booleanValue)
        .map(
            isRunning ->
                Health.up().withDetail(MESSAGE_KEY, "Update File Bundle disposable is running."))
        .orElse(Health.down().withDetail(MESSAGE_KEY, "Update File Bundle disposable has stopped."))
        .build();
  }

  Predicate<Disposable> isDisposableRunning = disposable -> !disposable.isDisposed();
}
