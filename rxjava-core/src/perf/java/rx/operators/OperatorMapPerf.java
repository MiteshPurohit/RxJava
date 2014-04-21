/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import java.util.concurrent.CountDownLatch;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;

public class OperatorMapPerf {

    @GenerateMicroBenchmark
    public void mapIdentityFunction(Input input) throws InterruptedException {
        input.observable.lift(MAP_OPERATOR).subscribe(input.observer);

        input.awaitCompletion();
    }

    private static final Func1<Integer, Integer> IDENTITY_FUNCTION = new Func1<Integer, Integer>() {
        @Override
        public Integer call(Integer value) {
            return value;
        }
    };

    private static final Operator<Integer, Integer> MAP_OPERATOR = new OperatorMap<Integer, Integer>(IDENTITY_FUNCTION);

    @State(Scope.Thread)
    public static class Input {

        @Param({ "1", "1024", "1048576" })
        public int size;

        public Observable<Integer> observable;
        public Observer<Integer> observer;

        private CountDownLatch latch;

        @Setup
        public void setup() {
            observable = Observable.create(new OnSubscribe<Integer>() {
                @Override
                public void call(Subscriber<? super Integer> o) {
                    for (int value = 0; value < size; value++) {
                        if (o.isUnsubscribed())
                            return;
                        o.onNext(value);
                    }
                    o.onCompleted();
                }
            });

            final BlackHole bh = new BlackHole();
            latch = new CountDownLatch(1);

            observer = new Observer<Integer>() {
                @Override
                public void onCompleted() {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable e) {
                    throw new RuntimeException(e);
                }

                @Override
                public void onNext(Integer value) {
                    bh.consume(value);
                }
            };

        }

        public void awaitCompletion() throws InterruptedException {
            latch.await();
        }
    }

}