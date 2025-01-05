package cn.gzten.rag.data.postgres.dao;

import com.pgvector.PGvector;
import jakarta.persistence.AttributeConverter;

import jakarta.persistence.Converter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Converter
@ConditionalOnProperty(value = "rag.storage.type", havingValue = "postgres")
public class PGVectorConverter implements AttributeConverter<float[], PGvector> {

    @Override
    public PGvector convertToDatabaseColumn(float[] attribute) {
        return attribute == null ? null : new PGvector(attribute);
    }

    @Override
    public float[] convertToEntityAttribute(PGvector dbData) {
        return dbData == null ? null : dbData.toArray();
    }
}
