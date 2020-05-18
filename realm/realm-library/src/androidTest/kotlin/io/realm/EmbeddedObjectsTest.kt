/*
 * Copyright 2020 Realm Inc.
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
package io.realm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.realm.entities.*
import io.realm.entities.embedded.*
import io.realm.kotlin.addChangeListener
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import io.realm.rule.BlockingLooperThread
import io.realm.rule.TestRealmConfigurationFactory
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.*
import kotlin.test.assertFailsWith

/**
 * Class testing the Embedded Objects feature.
 */
@RunWith(AndroidJUnit4::class)
class EmbeddedObjectsTest {

    @get:Rule
    val configFactory = TestRealmConfigurationFactory()

    private val looperThread = BlockingLooperThread()

    private lateinit var realmConfig: RealmConfiguration
    private lateinit var realm: Realm

    @Before
    fun setUp() {
        Realm.init(InstrumentationRegistry.getInstrumentation().targetContext)
        realmConfig = configFactory.createConfiguration()
        realm = Realm.getInstance(realmConfig)
    }

    @After
    fun tearDown() {
        if (this::realm.isInitialized) {
            realm.close()
        }
    }

    @Test
    @Ignore("Should we even support `createObject")
    fun createObject_noParentThrows() {
        // When using createObject, the parent Object must be provided
        realm.beginTransaction()
        assertFailsWith<IllegalArgumentException> { realm.createObject<EmbeddedSimpleChild>() }
    }

    @Test
    @Ignore("Should we even support `createObject")
    fun createObject_throwsIfParentHasMultipleFields() {
        // createObject is an akward API to use for Embedded Objects, so it doesn't support
        // parent objects which has multiple properties linking to it.
        realm.beginTransaction()
        val parent = realm.createObject(EmbeddedTreeParent::class.java, UUID.randomUUID().toString())
        assertFailsWith<IllegalArgumentException> { realm.createObject<EmbeddedTreeNode>(parent) }
    }

    @Test
    @Ignore("Should we even support `createObject")
    fun createObject_simpleSingleChild() {
        TODO()
    }

    @Test
    @Ignore("Should we even support `createObject")
    fun createObject_simpleChildList() {
        TODO()
    }

    @Test
    @Ignore("Should we even support `createObject")
    fun createObject_addToEndOfParentList() {
        // If the only link a parent has to an embedded child is through a list, any new children
        // are added to the end of that list.
        realm.beginTransaction()
        val parent = realm.createObject<EmbeddedSimpleListParent>(UUID.randomUUID().toString())
        parent.children.add(EmbeddedSimpleChild("1"))
        realm.createObject<EmbeddedSimpleChild>(parent)
        assertEquals(2, parent.children.size.toLong())
        assertNotEquals("1", parent.children.last()!!.id)
    }

    @Test
    @Ignore("Should we even support `createObject")
    fun createObject_treeSchema() {
        TODO()
    }

    @Test
    @Ignore("Should we even support `createObject")
    fun createObject_circularSchema() {
        TODO()
    }

    @Test
    fun settingParentFieldDeletesChild() {
        realm.executeTransaction {
            val parent = EmbeddedSimpleParent("parent")
            parent.child = EmbeddedSimpleChild("child")

            val managedParent: EmbeddedSimpleParent = it.copyToRealm(parent)
            val managedChild: EmbeddedSimpleChild = managedParent.child!!
            managedParent.child = null
            assertFalse(managedChild.isValid)
            assertEquals(0, realm.where<EmbeddedSimpleChild>().count())
        }
    }

    @Test
    fun settingUnmanagedEmbeddedObjectRefWillAutomaticallyCopyToRealm() {
        // similar to RealmList.add(), `=` will copy unmanaged embedded objects directly
        TODO()
    }

    @Test
    fun addingUnmanagedObjectAddEmbeddedObjectRefWillAutomaticallyCopyToRealm() {
        // similar to RealmList.add(), `=` will copy unmanaged embedded objects directly
        TODO()
    }

    @Test
    fun copyToRealmOrUpdate_deletesOldEmbeddedObject() {
    }

    @Test
    fun copyToRealm_noParentThrows() {
        realm.executeTransaction {
            assertFailsWith<IllegalArgumentException> {
                realm.copyToRealm(EmbeddedSimpleChild("child"))
            }
        }
    }

    @Test
    fun copyToRealmOrUpdate_throws() {
        realm.executeTransaction {
            assertFailsWith<IllegalArgumentException> {
                realm.copyToRealmOrUpdate(EmbeddedSimpleChild("child"))
            }
        }
    }

    @Test
    fun copyToRealm_simpleSingleChild() {
        realm.executeTransaction {
            val parent = EmbeddedSimpleParent("parent1")
            parent.child = EmbeddedSimpleChild("child1")
            it.copyToRealm(parent)
        }

        assertEquals(1, realm.where<EmbeddedSimpleParent>().count())
        assertEquals(1, realm.where<EmbeddedSimpleChild>().count())
    }

    @Test
    fun copyToRealm_simpleChildList() {
        realm.executeTransaction {
            val parent = EmbeddedSimpleListParent("parent1")
            parent.children = RealmList(EmbeddedSimpleChild("child1"))
            it.copyToRealm(parent)
        }

        assertEquals(1, realm.where<EmbeddedSimpleListParent>().count())
        assertEquals(1, realm.where<EmbeddedSimpleChild>().count())
    }

    @Test
    fun copyToRealm_treeSchema() {
        realm.executeTransaction {
            val parent = EmbeddedTreeParent("parent1")

            val node1 = EmbeddedTreeNode("node1")
            node1.leafNode = EmbeddedTreeLeaf("leaf1")
            parent.middleNode = node1
            val node2 = EmbeddedTreeNode("node2")
            node2.leafNodeList.add(EmbeddedTreeLeaf("leaf2"))
            node2.leafNodeList.add(EmbeddedTreeLeaf("leaf3"))
            parent.middleNodeList.add(node2)

            it.copyToRealm(parent)
        }

        assertEquals(1, realm.where<EmbeddedTreeParent>().count())
        assertEquals(2, realm.where<EmbeddedTreeNode>().count())
        assertEquals(3, realm.where<EmbeddedTreeLeaf>().count())
    }

    @Test
    fun copyToRealm_circularSchema() {
        realm.executeTransaction {
            val parent = EmbeddedCircularParent("parent")
            val child1 = EmbeddedCircularChild("child1")
            val child2 = EmbeddedCircularChild("child2")
            child1.singleChild = child2
            parent.singleChild = child1
            it.copyToRealm(parent)
        }

        assertEquals(1, realm.where<EmbeddedCircularParent>().count())
        assertEquals(2, realm.where<EmbeddedCircularChild>().count())
    }

    @Test
    fun copyToRealm_throwsIfMultipleRefsToSingleObjectsExists() {
        realm.executeTransaction { r ->
            val parent = EmbeddedCircularParent("parent")
            val child = EmbeddedCircularChild("child")
            child.singleChild = child // Create circle between children
            parent.singleChild = child
            assertFailsWith<IllegalArgumentException> { r.copyToRealm(parent) }
        }
    }

    @Test
    fun copyToRealm_throwsIfMultipleRefsToListObjectsExists() {
        realm.executeTransaction { r ->
            val parent = EmbeddedSimpleListParent("parent")
            val child = EmbeddedSimpleChild("child")
            parent.children = RealmList(child, child)
            assertFailsWith<IllegalArgumentException> { r.copyToRealm(parent) }
        }
    }

    @Test
    @Ignore("Add in another PR")
    fun insert_noParentThrows() {
        TODO()
    }

    @Test
    @Ignore("Add in another PR")
    fun insertOrUpdate_throws() {
        TODO()
    }

    @Test
    @Ignore("Add in another PR")
    fun insert_simpleSingleChild() {
        TODO()
    }

    @Test
    @Ignore("Add in another PR")
    fun insert_simpleChildList() {
        TODO()
    }

    @Test
    @Ignore("Add in another PR")
    fun insert_treeSchema() {
        TODO()
    }

    @Test
    @Ignore("Add in another PR")
    fun insert_circularSchema() {
        TODO()
    }

    @Test
    @Ignore("Add in another PR")
    fun insertOrUpdate_deletesOldEmbeddedObject() {
        TODO()
    }

    @Test
    @Ignore("Add in another PR")
    fun createObjectFromJson() {
        TODO("Placeholder for all tests regarding importing from JSON")
    }

    @Test
    @Ignore("Add in another PR")
    fun dynamicRealmObject_createEmbeddedObject() {
        TODO("Consider which kind of support there should be for embedded objets in DynamicRealm")
    }


    @Test
    fun realmObjectSchema_setEmbedded() {
        DynamicRealm.getInstance(realm.configuration).use { realm ->
            realm.executeTransaction {
                val objSchema: RealmObjectSchema = realm.schema[EmbeddedSimpleChild.NAME]!!
                assertTrue(objSchema.isEmbedded)
                objSchema.isEmbedded = false
                assertFalse(objSchema.isEmbedded)
                objSchema.isEmbedded = true
                assertTrue(objSchema.isEmbedded)
            }
        }
    }

    @Test
    fun realmObjectSchema_setEmbedded_throwsWithPrimaryKey() {
        DynamicRealm.getInstance(realm.configuration).use { realm ->
            realm.executeTransaction {
                val objSchema: RealmObjectSchema = realm.schema[AllJavaTypes.CLASS_NAME]!!
                assertFailsWith<IllegalStateException> { objSchema.isEmbedded = true }
            }
        }
    }

    @Test
    fun realmObjectSchema_setEmbedded_throwsIfBreaksParentInvariants() {
        // Classes can only be converted to be embedded if all objects have exactly one other
        // object pointing to it.
        DynamicRealm.getInstance(realm.configuration).use { realm ->
            realm.executeTransaction {

                // Create object with no parents
                realm.createObject(Dog.CLASS_NAME)
                val dogSchema = realm.schema[Dog.CLASS_NAME]!!
                // Succeed by mistake right now.
                // See https://github.com/realm/realm-core/issues/3729
                // The correct check is just below
                dogSchema.isEmbedded = true
                // assertFailsWith<IllegalStateException> {
                //    dogSchema.isEmbedded = true
                // }

                // Create object with two parents
                val cat: DynamicRealmObject = realm.createObject(Cat.CLASS_NAME)
                val owner1: DynamicRealmObject = realm.createObject(Owner.CLASS_NAME)
                owner1.setObject(Owner.FIELD_CAT, cat)
                val owner2: DynamicRealmObject = realm.createObject(Owner.CLASS_NAME)
                owner2.setObject(Owner.FIELD_CAT, cat)
                val catSchema = realm.schema[Cat.CLASS_NAME]!!
                assertFailsWith<IllegalStateException> {
                    catSchema.isEmbedded = true
                }
            }
        }
    }

    @Test
    fun realmObjectSchema_isEmbedded() {
        assertTrue(realm.schema[EmbeddedSimpleChild.NAME]!!.isEmbedded)
        assertFalse(realm.schema[AllTypes.CLASS_NAME]!!.isEmbedded)
    }

    // Check that deleting a non-embedded parent deletes all embedded children
    @Test
    fun deleteParentObject_deletesEmbeddedChildren() {
        realm.executeTransaction {
            val parent = EmbeddedSimpleParent("parent")
            parent.child = EmbeddedSimpleChild("child")

            val managedParent: EmbeddedSimpleParent = it.copyToRealm(parent)
            assertEquals(1, realm.where<EmbeddedSimpleChild>().count())
            val managedChild: EmbeddedSimpleChild = managedParent.child!!

            managedParent.deleteFromRealm()
            assertFalse(managedChild.isValid)
            assertEquals(0, realm.where<EmbeddedSimpleParent>().count())
            assertEquals(0, realm.where<EmbeddedSimpleChild>().count())
        }
    }

    // Check that deleting a embedded parent deletes all embedded children
    @Test
    fun deleteParentEmbeddedObject_deletesEmbeddedChildren() {
        realm.executeTransaction {
            val parent = EmbeddedTreeParent("parent1")
            val middleNode = EmbeddedTreeNode("node1")
            middleNode.leafNode = EmbeddedTreeLeaf("leaf1")
            middleNode.leafNodeList.add(EmbeddedTreeLeaf("leaf2"))
            middleNode.leafNodeList.add(EmbeddedTreeLeaf("leaf3"))
            parent.middleNode = middleNode

            val managedParent: EmbeddedTreeParent = it.copyToRealm(parent)
            assertEquals(1, realm.where<EmbeddedTreeNode>().count())
            assertEquals(3, realm.where<EmbeddedTreeLeaf>().count())
            managedParent.deleteFromRealm()
            assertEquals(0, realm.where<EmbeddedTreeNode>().count())
            assertEquals(0, realm.where<EmbeddedSimpleChild>().count())
        }
    }

    // Cascade deleting an embedded object will trigger its object listener.
    @Test
    fun deleteParent_triggerChildObjectNotifications() = looperThread.runBlocking {
        val realm = Realm.getInstance(realm.configuration)
        looperThread.closeAfterTest(realm)

        realm.executeTransaction {
            val parent = EmbeddedSimpleParent("parent")
            val child = EmbeddedSimpleChild("child")
            parent.child = child
            it.copyToRealm(parent)
        }

        val child = realm.where<EmbeddedSimpleParent>().findFirst()!!.child!!
        child.addChangeListener(RealmChangeListener<EmbeddedSimpleChild> {
            if (!it.isValid) {
                looperThread.testComplete()
            }
        })

        realm.executeTransaction {
            child.parent!!.deleteFromRealm()
        }
    }

    // Cascade deleting a parent will trigger the listener on any lists in child embedded
    // objects
    @Test
    fun deleteParent_triggerChildListObjectNotifications() = looperThread.runBlocking {
        val realm = Realm.getInstance(realm.configuration)
        looperThread.closeAfterTest(realm)

        realm.executeTransaction {
            val parent = EmbeddedSimpleListParent("parent")
            val child1 = EmbeddedSimpleChild("child1")
            val child2 = EmbeddedSimpleChild("child2")
            parent.children.add(child1)
            parent.children.add(child2)
            it.copyToRealm(parent)
        }

        val children: RealmList<EmbeddedSimpleChild> = realm.where<EmbeddedSimpleListParent>()
                .findFirst()!!
                .children

        children.addChangeListener { list ->
            if (!list.isValid) {
                looperThread.testComplete()
            }
        }

        realm.executeTransaction {
            realm.where<EmbeddedSimpleListParent>().findFirst()!!.deleteFromRealm()
        }
    }


    @Test
    @Ignore("Add in another PR")
    fun results_bulkUpdate() {
        // What happens if you bulk update a RealmResults. Should it be allowed to use embeded
        // objets here?
        TODO()
    }
}