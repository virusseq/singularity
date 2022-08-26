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

package org.cancogenvirusseq.singularity.config.kafka;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

@Slf4j
@Configuration
@Profile("kafka")
public class KafkaConsumerConfig {
  @Getter private final KafkaReceiver<String, String> receiver;

  public KafkaConsumerConfig(KafkaProperties properties) {
    receiver = KafkaReceiver.create(buildOptions(properties));
  }

  private ReceiverOptions<String, String> buildOptions(KafkaProperties properties) {
    return ReceiverOptions.<String, String>create(
            Map.ofEntries(
                new AbstractMap.SimpleEntry<>(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServer()),
                new AbstractMap.SimpleEntry<>(
                    ConsumerConfig.CLIENT_ID_CONFIG, properties.getClientId()),
                new AbstractMap.SimpleEntry<>(
                    ConsumerConfig.GROUP_ID_CONFIG, properties.getGroupId()),
                new AbstractMap.SimpleEntry<>(
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class),
                new AbstractMap.SimpleEntry<>(
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class),
                new AbstractMap.SimpleEntry<>(
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, properties.getAutoOffsetReset())))
        //.subscription(Collections.singleton(properties.getTopic()))
        .subscription(Collections.singleton("release_archive"))
        .addAssignListener(partitions -> log.debug("onPartitionsAssigned {}", partitions))
        .addRevokeListener(partitions -> log.debug("onPartitionsRevoked {}", partitions));
  }
}
