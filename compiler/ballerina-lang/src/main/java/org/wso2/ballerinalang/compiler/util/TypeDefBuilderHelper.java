/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.ballerinalang.compiler.util;

import io.ballerina.tools.diagnostics.Location;
import org.ballerinalang.model.TreeBuilder;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.elements.MarkdownDocAttachment;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.types.TypeKind;
import org.wso2.ballerinalang.compiler.desugar.ASTBuilderUtil;
import org.wso2.ballerinalang.compiler.parser.BLangAnonymousModelHelper;
import org.wso2.ballerinalang.compiler.semantics.analyzer.Types;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolEnv;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAttachedFunction;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BObjectTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BRecordTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BStructureTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BTypeDefinitionSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SymTag;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.semantics.model.types.BErrorType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BField;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BNoType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BObjectType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BRecordType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BStructureType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.tree.BLangClassDefinition;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangIdentifier;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTypeDefinition;
import org.wso2.ballerinalang.compiler.tree.types.BLangBuiltInRefTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangConstrainedType;
import org.wso2.ballerinalang.compiler.tree.types.BLangErrorType;
import org.wso2.ballerinalang.compiler.tree.types.BLangObjectTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangRecordTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangStructureTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangType;
import org.wso2.ballerinalang.compiler.tree.types.BLangUserDefinedType;
import org.wso2.ballerinalang.util.Flags;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.ballerinalang.model.symbols.SymbolOrigin.SOURCE;
import static org.ballerinalang.model.symbols.SymbolOrigin.VIRTUAL;
import static org.wso2.ballerinalang.compiler.desugar.ASTBuilderUtil.createIdentifier;

/**
 * Helper class with util methods to create type definitions.
 *
 * @since 1.2.0
 */
public class TypeDefBuilderHelper {

    public static final String INTERSECTED_ERROR_DETAIL = "$IntersectedErrorDetail$";

    public static BLangRecordTypeNode createRecordTypeNode(BRecordType recordType, PackageID packageID,
                                                           SymbolTable symTable, Location pos) {
        List<BLangSimpleVariable> fieldList = new ArrayList<>();
        for (BField field : recordType.fields.values()) {
            BVarSymbol symbol = field.symbol;
            if (symbol == null) {
                symbol = new BVarSymbol(Flags.PUBLIC, field.name, packageID, symTable.pureType, null, field.pos,
                                        VIRTUAL);
            }

            BLangSimpleVariable fieldVar = ASTBuilderUtil.createVariable(field.pos, symbol.name.value, field.type,
                                                                         null, symbol);
            fieldList.add(fieldVar);
        }
        return createRecordTypeNode(fieldList, recordType, pos);
    }

    public static BLangObjectTypeNode createObjectTypeNode(BObjectType objectType, Location pos) {
        List<BLangSimpleVariable> fieldList = new ArrayList<>();
        for (BField field : objectType.fields.values()) {
            BVarSymbol symbol = field.symbol;
            BLangSimpleVariable fieldVar = ASTBuilderUtil.createVariable(field.pos, symbol.name.value, field.type,
                                                                         null, symbol);
            fieldList.add(fieldVar);
        }
        return createObjectTypeNode(fieldList, objectType, pos);
    }

    public static BLangRecordTypeNode createRecordTypeNode(List<BLangSimpleVariable> typeDefFields,
                                                           BRecordType recordType, Location pos) {
        BLangRecordTypeNode recordTypeNode = (BLangRecordTypeNode) TreeBuilder.createRecordTypeNode();
        recordTypeNode.setBType(recordType);
        recordTypeNode.fields = typeDefFields;
        recordTypeNode.symbol = recordType.tsymbol;
        recordTypeNode.pos = pos;

        return recordTypeNode;
    }

    public static BLangObjectTypeNode createObjectTypeNode(List<BLangSimpleVariable> typeDefFields,
                                                           BObjectType objectType, Location pos) {
        BLangObjectTypeNode objectTypeNode = (BLangObjectTypeNode) TreeBuilder.createObjectTypeNode();
        objectTypeNode.setBType(objectType);
        objectTypeNode.fields = typeDefFields;
        objectTypeNode.symbol = objectType.tsymbol;
        objectTypeNode.pos = pos;

        return objectTypeNode;
    }

    public static BLangFunction createInitFunctionForRecordType(BLangRecordTypeNode recordTypeNode, SymbolEnv env,
                                                                Names names, SymbolTable symTable) {
        BLangFunction initFunction = createInitFunctionForStructureType(recordTypeNode.pos, recordTypeNode.symbol, env,
                                                                        names, Names.INIT_FUNCTION_SUFFIX, symTable,
                                                                        recordTypeNode.getBType());
        BStructureTypeSymbol structureSymbol = ((BStructureTypeSymbol) recordTypeNode.getBType().tsymbol);
        structureSymbol.initializerFunc = new BAttachedFunction(initFunction.symbol.name, initFunction.symbol,
                                                                (BInvokableType) initFunction.getBType(),
                                                                initFunction.pos);
        recordTypeNode.initFunction = initFunction;
        structureSymbol.scope.define(structureSymbol.initializerFunc.symbol.name,
                                     structureSymbol.initializerFunc.symbol);
        return initFunction;
    }

    public static BLangFunction createInitFunctionForStructureType(Location location,
                                                                   BSymbol symbol,
                                                                   SymbolEnv env,
                                                                   Names names,
                                                                   Name suffix,
                                                                   SymbolTable symTable,
                                                                   BType type) {
        return createInitFunctionForStructureType(location, symbol, env, names, suffix, type, symTable.nilType);
    }

    public static BLangFunction createInitFunctionForStructureType(Location location,
                                                                   BSymbol symbol,
                                                                   SymbolEnv env,
                                                                   Names names,
                                                                   Name suffix,
                                                                   BType type,
                                                                   BType returnType) {
        String structTypeName = type.tsymbol.name.value;
        BLangFunction initFunction = ASTBuilderUtil
                .createInitFunctionWithNilReturn(location, structTypeName, suffix);

        // Create the receiver and add receiver details to the node
        initFunction.receiver = ASTBuilderUtil.createReceiver(location, type);
        BVarSymbol receiverSymbol = new BVarSymbol(Flags.asMask(EnumSet.noneOf(Flag.class)),
                                                   names.fromIdNode(initFunction.receiver.name),
                                                   names.originalNameFromIdNode(initFunction.receiver.name),
                                                   env.enclPkg.symbol.pkgID, type, null, location, VIRTUAL);
        initFunction.receiver.symbol = receiverSymbol;
        initFunction.attachedFunction = true;
        initFunction.flagSet.add(Flag.ATTACHED);

        // Create the function type
        initFunction.setBType(new BInvokableType(new ArrayList<>(), returnType, null));

        // Create the function symbol
        Name funcSymbolName = names.fromString(Symbols.getAttachedFuncSymbolName(structTypeName, suffix.value));
        initFunction.symbol = Symbols
                .createFunctionSymbol(Flags.asMask(initFunction.flagSet), funcSymbolName, funcSymbolName,
                                      env.enclPkg.symbol.pkgID, initFunction.getBType(), symbol,
                                      initFunction.body != null, initFunction.pos, VIRTUAL);
        initFunction.symbol.scope = new Scope(initFunction.symbol);
        initFunction.symbol.scope.define(receiverSymbol.name, receiverSymbol);
        initFunction.symbol.receiverSymbol = receiverSymbol;
        initFunction.name = createIdentifier(location, funcSymbolName.value);

        // Create the function type symbol
        BInvokableTypeSymbol tsymbol = Symbols.createInvokableTypeSymbol(SymTag.FUNCTION_TYPE,
                                                                         initFunction.symbol.flags,
                                                                         env.enclPkg.packageID, initFunction.getBType(),
                                                                         initFunction.symbol, initFunction.pos,
                                                                         VIRTUAL);
        tsymbol.params = initFunction.symbol.params;
        tsymbol.restParam = initFunction.symbol.restParam;
        tsymbol.returnType = initFunction.symbol.retType;
        initFunction.getBType().tsymbol = tsymbol;

        receiverSymbol.owner = initFunction.symbol;

        // Add return type to the symbol
        initFunction.symbol.retType = returnType;

        return initFunction;
    }

    public static BLangTypeDefinition addTypeDefinition(BType type, BTypeSymbol symbol, BLangType typeNode,
                                                        SymbolEnv env) {
        BLangTypeDefinition typeDefinition = (BLangTypeDefinition) TreeBuilder.createTypeDefinition();
        typeDefinition.typeNode = typeNode;
        typeDefinition.setBType(type);
        typeDefinition.symbol = symbol;
        typeDefinition.name = createIdentifier(symbol.pos, symbol.name.value);
        if (env != null) {
            env.enclPkg.addTypeDefinition(typeDefinition);
        }
        return typeDefinition;
    }

    public static BLangTypeDefinition createTypeDefinitionForTSymbol(BType type, BSymbol symbol, BLangType typeNode,
                                                                     SymbolEnv env) {
        BLangTypeDefinition typeDefinition = (BLangTypeDefinition) TreeBuilder.createTypeDefinition();
        typeDefinition.typeNode = typeNode;
        typeDefinition.setBType(type);
        typeDefinition.symbol = Symbols.createTypeDefinitionSymbol(symbol.flags,
                    Names.fromString(symbol.name.value), symbol.pkgID, type, symbol.owner,
                    symbol.pos, symbol.origin);
        typeDefinition.name = createIdentifier(symbol.pos, symbol.name.value);
        env.enclPkg.addTypeDefinition(typeDefinition);
        return typeDefinition;
    }

    public static BLangClassDefinition createClassDef(Location pos, BObjectTypeSymbol classTSymbol,
                                                      SymbolEnv env) {
        BObjectType objType = (BObjectType) classTSymbol.type;
        List<BLangSimpleVariable> fieldList = new ArrayList<>();
        for (BField field : objType.fields.values()) {
            BVarSymbol symbol = field.symbol;
            BLangSimpleVariable fieldVar = ASTBuilderUtil.createVariable(field.pos, symbol.name.value, field.type,
                                                                         null, symbol);
            fieldList.add(fieldVar);
        }

        BLangClassDefinition classDefNode = (BLangClassDefinition) TreeBuilder.createClassDefNode();
        classDefNode.setBType(objType);
        classDefNode.fields = fieldList;
        classDefNode.symbol = classTSymbol;
        classDefNode.pos = pos;

        env.enclPkg.addClassDefinition(classDefNode);

        return classDefNode;
    }

    public static BLangErrorType createBLangErrorType(Location pos, BErrorType type, SymbolEnv env,
                                                      BLangAnonymousModelHelper anonymousModelHelper) {
        BLangErrorType errorType = (BLangErrorType) TreeBuilder.createErrorTypeNode();
        errorType.setBType(type);

        BLangUserDefinedType userDefinedTypeNode = (BLangUserDefinedType) TreeBuilder.createUserDefinedTypeNode();
        userDefinedTypeNode.pos = pos;
        userDefinedTypeNode.pkgAlias = (BLangIdentifier) TreeBuilder.createIdentifierNode();

        BType detailType = Types.getImpliedType(type.detailType);

        if (detailType.tag == TypeTags.MAP) {
            BLangBuiltInRefTypeNode refType = (BLangBuiltInRefTypeNode) TreeBuilder.createBuiltInReferenceTypeNode();
            refType.typeKind = TypeKind.MAP;
            refType.pos = pos;

            BLangConstrainedType constrainedType = (BLangConstrainedType) TreeBuilder.createConstrainedTypeNode();
            constrainedType.constraint = userDefinedTypeNode; // We need to catch this and override the type-resolving
            userDefinedTypeNode.typeName = createIdentifier(pos, INTERSECTED_ERROR_DETAIL);
            constrainedType.type = refType;
            constrainedType.pos = pos;

            errorType.detailType = constrainedType;
            return errorType;
        }

        String typeName = detailType.tsymbol != null
                ? detailType.tsymbol.name.value
                : anonymousModelHelper.getNextAnonymousIntersectionErrorDetailTypeName(env.enclPkg.packageID);

        userDefinedTypeNode.typeName = createIdentifier(pos, typeName);
        userDefinedTypeNode.setBType(detailType);
        errorType.detailType = userDefinedTypeNode;

        return errorType;
    }

    public static String getPackageAlias(SymbolEnv env, String compUnitName, PackageID typePkgId) {
        for (BLangImportPackage importStmt : env.enclPkg.imports) {
            if (importStmt == null || importStmt.compUnit == null || importStmt.compUnit.value == null ||
                    !importStmt.compUnit.value.equals(compUnitName)) {
                continue;
            }

            if (importStmt.symbol != null && typePkgId.equals(importStmt.symbol.pkgID)) {
                return importStmt.alias.value;
            }
        }

        return ""; // current module
    }

    public static void populateStructureFields(Types types, SymbolTable symTable,
                                               BLangAnonymousModelHelper anonymousModelHelper, Names names,
                                               BLangStructureTypeNode structureTypeNode, BStructureType structureType,
                                               BStructureType origStructureType, Location pos, SymbolEnv env,
                                               PackageID pkgID, Set<BType> unresolvedTypes,
                                               long flag, boolean isImmutable) {
        BTypeSymbol structureSymbol = structureType.tsymbol;
        LinkedHashMap<String, BField> fields = new LinkedHashMap<>();
        for (BField origField : origStructureType.fields.values()) {
            BType fieldType;
            if (isImmutable) {
                fieldType = ImmutableTypeCloner.getImmutableType(pos, types, origField.type, env,
                        env.enclPkg.packageID, env.scope.owner, symTable, anonymousModelHelper, names, unresolvedTypes);
            } else {
                fieldType = origField.type;
            }

            Name origFieldName = origField.symbol.originalName;
            Name fieldName = origField.name;
            BVarSymbol fieldSymbol;
            BType referredType = Types.getImpliedType(fieldType);
            if (referredType.tag == TypeTags.INVOKABLE && referredType.tsymbol != null) {
                fieldSymbol = new BInvokableSymbol(origField.symbol.tag, origField.symbol.flags | flag,
                        fieldName, origFieldName, pkgID, fieldType,
                        structureSymbol, origField.symbol.pos, SOURCE);
                BInvokableTypeSymbol tsymbol = (BInvokableTypeSymbol) referredType.tsymbol;
                BInvokableSymbol invokableSymbol = (BInvokableSymbol) fieldSymbol;
                invokableSymbol.params = tsymbol.params == null ? null : new ArrayList<>(tsymbol.params);
                invokableSymbol.restParam = tsymbol.restParam;
                invokableSymbol.retType = tsymbol.returnType;
                invokableSymbol.flags = tsymbol.flags;
            } else if (fieldType == symTable.semanticError) {
                // Can only happen for records.
                fieldSymbol = new BVarSymbol(origField.symbol.flags | flag | Flags.OPTIONAL,
                        fieldName, origFieldName, pkgID, symTable.neverType,
                        structureSymbol, origField.symbol.pos, SOURCE);
            } else {
                fieldSymbol = new BVarSymbol(origField.symbol.flags | flag, fieldName, origFieldName, pkgID,
                        fieldType, structureSymbol,
                        origField.symbol.pos, SOURCE);
            }
            String nameString = fieldName.value;
            fields.put(nameString, new BField(fieldName, null, fieldSymbol));
            structureSymbol.scope.define(fieldName, fieldSymbol);
        }
        structureType.fields = fields;

        if (origStructureType.tag == TypeTags.OBJECT) {
            return;
        }
        BLangUserDefinedType origTypeRef = new BLangUserDefinedType(
                ASTBuilderUtil.createIdentifier(pos,
                        TypeDefBuilderHelper.getPackageAlias(env, null,
                                origStructureType.tsymbol.pkgID)),
                ASTBuilderUtil.createIdentifier(pos, origStructureType.tsymbol.name.value));
        origTypeRef.pos = pos;
        origTypeRef.setBType(origStructureType);

        if (isImmutable) {
            structureTypeNode.typeRefs.add(origTypeRef);
        }
    }

    public static void createTypeDefinition(BRecordType type, Location pos, Names names,
                                            Types types, SymbolTable symTable,
                                            SymbolEnv env) {
        BRecordTypeSymbol recordSymbol = (BRecordTypeSymbol) type.tsymbol;

        BTypeDefinitionSymbol typeDefinitionSymbol = Symbols.createTypeDefinitionSymbol(type.tsymbol.flags,
                type.tsymbol.name, env.scope.owner.pkgID, null, env.scope.owner, pos, VIRTUAL);
        typeDefinitionSymbol.scope = new Scope(typeDefinitionSymbol);
        typeDefinitionSymbol.scope.define(Names.fromString(typeDefinitionSymbol.name.value), typeDefinitionSymbol);

        type.tsymbol.scope = new Scope(type.tsymbol);
        for (BField field : ((HashMap<String, BField>) type.fields).values()) {
            type.tsymbol.scope.define(field.name, field.symbol);
            field.symbol.owner = recordSymbol;
        }
        typeDefinitionSymbol.type = type;
        recordSymbol.type = type;
        recordSymbol.typeDefinitionSymbol = typeDefinitionSymbol;
        recordSymbol.markdownDocumentation = new MarkdownDocAttachment(0);

        BLangRecordTypeNode recordTypeNode = TypeDefBuilderHelper.createRecordTypeNode(new ArrayList<>(), type,
                pos);
        TypeDefBuilderHelper.populateStructureFields(types, symTable, null, names, recordTypeNode, type, type, pos,
                env, env.scope.owner.pkgID, null, Flags.REQUIRED, false);
        recordTypeNode.sealed = true;
        recordTypeNode.analyzed = true;
        type.restFieldType = new BNoType(TypeTags.NONE);
        BLangTypeDefinition typeDefinition = TypeDefBuilderHelper.createTypeDefinitionForTSymbol(null,
                typeDefinitionSymbol, recordTypeNode, env);
        typeDefinition.symbol.scope = new Scope(typeDefinition.symbol);
        typeDefinition.symbol.type = type;
        typeDefinition.flagSet = new HashSet<>();
        typeDefinition.flagSet.add(Flag.PUBLIC);
        typeDefinition.flagSet.add(Flag.ANONYMOUS);
    }
}
