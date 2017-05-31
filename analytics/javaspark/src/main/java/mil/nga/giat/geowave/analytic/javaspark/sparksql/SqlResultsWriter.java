package mil.nga.giat.geowave.analytic.javaspark.sparksql;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

import mil.nga.giat.geowave.adapter.vector.FeatureDataAdapter;
import mil.nga.giat.geowave.analytic.javaspark.sparksql.util.GeomReader;
import mil.nga.giat.geowave.analytic.javaspark.sparksql.util.GeomWriter;
import mil.nga.giat.geowave.analytic.javaspark.sparksql.util.SchemaConverter;
import mil.nga.giat.geowave.core.geotime.ingest.SpatialDimensionalityTypeProvider;
import mil.nga.giat.geowave.core.store.DataStore;
import mil.nga.giat.geowave.core.store.IndexWriter;
import mil.nga.giat.geowave.core.store.adapter.exceptions.MismatchedIndexToAdapterMapping;
import mil.nga.giat.geowave.core.store.index.PrimaryIndex;
import mil.nga.giat.geowave.core.store.operations.remote.options.DataStorePluginOptions;

public class SqlResultsWriter
{
	private final static Logger LOGGER = LoggerFactory.getLogger(SqlResultsWriter.class);

	private final Dataset<Row> results;
	private final DataStorePluginOptions outputDataStore;
	private final NumberFormat nf;

	public SqlResultsWriter(
			Dataset<Row> results,
			DataStorePluginOptions outputDataStore ) {
		this.results = results;
		this.outputDataStore = outputDataStore;

		this.nf = NumberFormat.getIntegerInstance();
		this.nf.setMinimumIntegerDigits(6);
	}

	public void writeResults(
			String typeName ) {
		StructType schema = results.schema();
		SimpleFeatureType featureType = SchemaConverter.schemaToFeatureType(
				schema,
				typeName);

		final SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(
				featureType);

		final FeatureDataAdapter featureAdapter = new FeatureDataAdapter(
				featureType);

		final DataStore featureStore = outputDataStore.createDataStore();
		final PrimaryIndex featureIndex = new SpatialDimensionalityTypeProvider().createPrimaryIndex();

		GeomReader geomReader = new GeomReader();

		try (IndexWriter writer = featureStore.createWriter(
				featureAdapter,
				featureIndex)) {

			List<Row> rows = results.collectAsList();

			for (int r = 0; r < rows.size(); r++) {
				Row row = rows.get(r);

				for (int i = 0; i < schema.fields().length; i++) {
					StructField field = schema.apply(i);

					if (field.name().equals(
							"geom")) {
						try {
							Geometry geom = geomReader.read((String) row.apply(i));

							sfBuilder.set(
									"geom",
									geom);
						}
						catch (ParseException e) {
							LOGGER.error(e.getMessage());
						}
					}
					else if (field.dataType() == DataTypes.TimestampType) {
						long millis = ((Timestamp) row.apply(i)).getTime();
						Date date = new Date(
								millis);

						sfBuilder.set(
								field.name(),
								date);
					}
					else {
						sfBuilder.set(
								field.name(),
								row.apply(i));
					}
				}

				final SimpleFeature sf = sfBuilder.buildFeature("result-" + nf.format(r));

				writer.write(sf);
			}
		}
		catch (final MismatchedIndexToAdapterMapping e) {
			LOGGER.error(
					e.getMessage(),
					e);
		}
		catch (final IOException e) {
			LOGGER.error(
					e.getMessage(),
					e);
		}
	}
}
