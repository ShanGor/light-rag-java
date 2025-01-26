package cn.gzten.rag.data.postgres.dao;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static cn.gzten.rag.util.LightRagUtils.stringToVector;
import static cn.gzten.rag.util.LightRagUtils.vectorToString;

@Converter
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PGVectorConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        return vectorToString(attribute);
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        return stringToVector(dbData);
    }
}
