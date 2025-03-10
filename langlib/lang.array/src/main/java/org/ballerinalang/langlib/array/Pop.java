/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.langlib.array;

import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;

import static org.ballerinalang.langlib.array.utils.ArrayUtils.checkIsArrayOnlyOperation;

/**
 * Native implementation of lang.array:pop((any|error)[]).
 *
 * @since 1.0
 */
//@BallerinaFunction(
//        orgName = "ballerina", packageName = "lang.array", functionName = "pop",
//        args = {@Argument(name = "arr", type = TypeKind.ARRAY)},
//        returnType = {@ReturnType(type = TypeKind.ANY)},
//        isPublic = true
//)
public class Pop {

    private static final String FUNCTION_SIGNATURE = "pop()";

    public static Object pop(BArray arr) {
        checkIsArrayOnlyOperation(TypeUtils.getImpliedType(arr.getType()), FUNCTION_SIGNATURE);
        return arr.shift(arr.size() - 1);
    }
}
