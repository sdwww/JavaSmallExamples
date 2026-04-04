package com.demo;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class GQLDemo {

    public static void main(String[] args) throws Exception {
        String schema = readFileToString("./src/main/resource/demo.graphql");
        String query = readFileToString("./src/main/resource/demo_query.graphql");

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder.dataFetcher("user", env -> new Object()))
                .type("User", builder -> builder.dataFetcher("id", env -> 123))
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        GraphQL build = GraphQL.newGraphQL(graphQLSchema)
                .defaultDataFetcherExceptionHandler(new DataFetcherExceptionHandler() {
                    @Override
                    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(DataFetcherExceptionHandlerParameters handlerParameters) {
                        return null;
                    }
                })
                .build();
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .build();
        ExecutionResult executionResult = build.execute(executionInput);

        System.out.println(executionResult.getData().toString());
    }

    /**
     * 读取文件内容并返回字符串（Java 11+）
     *
     * @param filePath 文件的绝对路径或相对路径（例如："D:/test.txt" 或 "./src/test.txt"）
     * @return 文件的完整内容字符串
     * @throws Exception 读取文件时的IO异常（如文件不存在、权限不足等）
     */
    private static String readFileToString(String filePath) throws Exception {
        // 1. 将文件路径转换为Path对象（Java NIO的核心类）
        Path file = Paths.get(filePath);

        // 2. 读取文件内容为字符串，指定UTF-8编码（避免中文乱码）
        // StandardCharsets.UTF_8 确保跨平台编码一致性
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
